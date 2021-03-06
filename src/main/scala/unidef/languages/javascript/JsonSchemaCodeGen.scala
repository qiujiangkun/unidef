package unidef.languages.javascript

import com.typesafe.scalalogging.Logger
import io.circe.{Json, JsonObject}
import unidef.common.NamingConvention
import unidef.common.ast.{AstFunctionDecl, extractArgumentStruct}
import unidef.common.ty.*
import unidef.utils.{ParseCodeException, TypeEncodeException}

import scala.collection.mutable

class JsonSchemaCodeGenOption(
    val naming: NamingConvention = JsonNamingConvention,
    val useListForJsonAny: Boolean = false,
    val useCustomFormat: Boolean = false
)

class JsonSchemaCodeGen(options: JsonSchemaCodeGenOption = JsonSchemaCodeGenOption())
    extends TypeEncoder[Json] {
  val logger: Logger = Logger[this.type]

  def generateFuncDecl(func: AstFunctionDecl): Json = {
    val struct = extractArgumentStruct(func)
    generateType(struct, isMethodParameters = true)
  }

  def convertToMatrix(struct0: TyStruct): TyStructImpl = {
    val fields = mutable.ArrayBuffer[TyField]()
    val headers = mutable.ArrayBuffer[String]()
    val body_row = mutable.ArrayBuffer[TyNode]()

    struct0.fields.get.foreach {
      case f: TyField if f.value.isInstanceOf[TyList] =>
        headers += f.name.get
        body_row += f.value.asInstanceOf[TyList].value
      case x: TyField =>
        fields += x
    }
    fields += TyFieldBuilder().name("headers").value(TyConstTupleString(headers.toList)).build()
    fields += TyFieldBuilder().name("body").value(Types.list(TyTupleImpl(body_row.toList))).build()
    // TODO: add header names and types
    TyStructBuilder().fields(fields.toList).build()
  }

  def jsonObjectOf(ty: String, others: (String, Json)*): Json = {
    Json.fromFields(
      Seq("type" -> Json.fromString(ty))
        ++
          others.map(x => x._1 -> x._2)
    )
  }

  override def encode(ty: TyNode): Option[Json] = {
    val coded = ty match {
      case _: TyString => Some(jsonObjectOf("string"))
      case _: TyInteger =>
        Some(jsonObjectOf("integer"))
      case _: TyFloat =>
        Some(jsonObjectOf("number"))
      case _: TyNumeric =>
        Some(jsonObjectOf("number"))
      case _: TyBoolean =>
        Some(jsonObjectOf("boolean"))
      case _: TyDateTime =>
        Some(jsonObjectOf("string", "format" -> Json.fromString("datetime")))

      case t: TyTimeStamp =>
        Some(
          jsonObjectOf(
            "string",
            "format" -> Json.fromString("timestamp"),
            "unit" -> t.timeUnit
              .map(_.toString)
              .map(Json.fromString)
              .getOrElse(Json.Null)
          )
        )
      case x: TyConstTupleString =>
        Some(
          jsonObjectOf(
            "array",
            "items" -> Json.fromValues(
              x.values.map(name =>
                Json.obj(
                  "const" -> Json.fromString(name)
                )
              )
            ),
            "minItems" -> Json.fromInt(x.values.size),
            "maxItems" -> Json.fromInt(x.values.size)
          )
        )
      case x: TyTuple =>
        Some(
          jsonObjectOf(
            "array",
            "items" -> Json.fromValues(x.values.map(generateType(_))),
            "minItems" -> Json.fromInt(x.values.size),
            "maxItems" -> Json.fromInt(x.values.size)
          )
        )
      case x: TyUnion if x.values.map(x => x.isInstanceOf[TyStruct]).forall(identity) =>
        Some(jsonObjectOf("object", "oneOf" -> Json.fromValues(x.values.map(generateType(_)))))
      case x: TyUnion if !x.values.map(x => x.isInstanceOf[TyStruct]).exists(identity) =>
        Some(Json.obj("type" -> Json.fromValues(x.values.map(generateType(_)))))

      case x: TyUnion =>
        logger.warn("Failed to encode union: " + x.values)
        Some(Json.obj())
      case x: TyList =>
        Some(jsonObjectOf("array", "items" -> generateType(x.value)))

      case x: TyEnum if x.name.isDefined =>
        Some(
          Json.obj(
            "enum" -> Json
              .fromValues(
                x.variants
                  .map(_.names.head)
                  .map(options.naming.toEnumValueName)
                  .map(Json.fromString)
              ),
            "name" -> Json.fromString(options.naming.toClassName(x.name.get))
          )
        )
      case x: TyEnum =>
        Some(
          Json.obj(
            "enum" -> Json
              .fromValues(
                x.variants
                  .map(_.names.head)
                  .map(options.naming.toEnumValueName)
                  .map(Json.fromString)
              )
          )
        )

      case x: TyStruct if x.fields.isDefined =>
        // TODO: pass parameters here
        val keyRequired = true
        val keyAdditionalProperties = false
        val isMethodParameters = true
        val naming = if (isMethodParameters) {
          options.naming.toFunctionParameterName
        } else {
          options.naming.toFieldName
        }
        val others: mutable.Map[String, Json] = mutable.Map.empty
        others += "properties" -> Json.fromFields(
          x.fields.get.map(f =>
            naming(f.name.get) -> generateType(f.value match {
              case opt: TyOption => opt.value
              case x => x
            })
          )
        )

        if (!keyAdditionalProperties) {
          others += "additionalProperties" -> Json.False
        }
        if (keyRequired) {
          others += "required" -> Json.fromValues(
            x.fields.get
              .filterNot(f => f.value.isInstanceOf[TyOption])
              .map(f => naming(f.name.get))
              .map(Json.fromString)
          )

        }
        Some(
          jsonObjectOf(
            "object",
            others.toSeq: _*
          )
        )
      case _: TyJsonObject => Some(jsonObjectOf("object"))
      case _: TyStruct => Some(jsonObjectOf("object"))
      case _: TyAny | _: TyJsonAny if options.useListForJsonAny =>
        Some(
          Json.fromValues(
            Seq("number", "string", "boolean", "object", "array", "null").map(Json.fromString)
          )
        )
      case _: TyAny | _: TyJsonAny if !options.useListForJsonAny =>
        Some(Json.fromJsonObject(JsonObject.empty))
      case x: TyNamed =>
        Some(jsonObjectOf("string", "name" -> Json.fromString(x.ref)))
      case _: TyByteArray =>
        Some(
          jsonObjectOf(
            "string",
            (if (options.useCustomFormat) "format" else "$comment") -> Json.fromString("bytes")
          )
        )
      case _: TyInet => Some(jsonObjectOf("string", "format" -> Json.fromString("hostname")))
      case _: TyUuid => Some(jsonObjectOf("string", "format" -> Json.fromString("uuid")))
      case _: TyNull => Some(jsonObjectOf("null"))
      case _ => None
    }
    ty match {
//      case ty: TyCommentable if ty.getComment != "" =>
//        coded
//          .flatMap(x => x.asObject)
//          .map(x => x.add("$comment", Json.fromString(ty.getComment)))
//          .map(Json.fromJsonObject)
      case _ => coded
    }
  }
  def generateType(ty: TyNode, isMethodParameters: Boolean = false): Json = {
    val new_ty = ty match {
      case x: TyStruct if x.dataframe.contains(true) =>
        convertToMatrix(x)
      case x => x
    }
    encodeOrThrow(new_ty, "json schema")
  }

}
