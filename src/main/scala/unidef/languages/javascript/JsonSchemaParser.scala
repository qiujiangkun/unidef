package unidef.languages.javascript

import io.circe.Json.Folder
import io.circe.*
import unidef.common.ast.{AstFunctionDecl, AstLiteralString, AstRawCode}
import unidef.common.ty.*
import unidef.common.ast.*
import unidef.utils.FileUtils.readFile
import unidef.utils.JsonUtils.{getList, getObject, getString, iterateOver}
import unidef.utils.{ParseCodeException, TypeDecodeException}

import scala.collection.mutable
class JsonSchemaParserOption(
    val extendedGrammar: Boolean = true
)
class JsonSchemaParser(options: JsonSchemaParserOption = JsonSchemaParserOption()) {
  val jsonSchemaCommon: JsonSchemaCommon = JsonSchemaCommon(options.extendedGrammar)

  def parseFunction(content: JsonObject): AstFunctionDecl = {
    val name = getString(content, "name")
    val parameters = content("parameters")
      .map(_ => getList(content, "parameters"))
      .getOrElse(Vector())
      .map(parseFieldType)

    val ret = content("return")
      .map(x =>
        if (x.isString)
          jsonSchemaCommon.decodeOrThrow(x.asString.get)
        else
          TyStructBuilder()
            .fields(iterateOver(x, "name", "type").map { (name, json) =>
              val ty = parse(Json.fromJsonObject(json))
              TyFieldBuilder().name(name).value(ty).build()
            }.toList)
            .build()
      )
      .getOrElse(Types.unit())

    val builder =
      AstFunctionDeclBuilder()
        .name(name)
        .parameters(
          Asts.parameters(
            parameters
              .map(x => AstValDefBuilder().name(x.name.get).ty(x.value).build())
              .toList
          )
        )
        .returnType(ret)

    if (content("language").isDefined && content("body").isDefined) {
      val language = getString(content, "language")
      val body = getString(content, "body")
      builder.body(AstRawCodeImpl(body, Some(language)))
    }

    builder.build()
  }

  def parseStruct(value: JsonObject): TyStruct = {
    val fields =
      value("properties")
        .orElse(if (options.extendedGrammar) value("fields") else None)
        .map(x =>
          // check extended
          iterateOver(x, "name", "type")
            .map { (name, json) =>
              val ty = parse(Json.fromJsonObject(json))
              TyFieldBuilder().name(name).value(ty).build()
            }
        )
        // TODO TyNamed or TyStruct?
        .getOrElse(Nil)

    val node = TyStructBuilder().fields(fields.toList)
//    collectExtKeys(value, extKeysForClassDecl.toList).foreach(node.setValue)
    val comment = value("comment").orElse(value("$comment"))
    if (comment.isDefined)
      node.comment(getString(value, "comment"))

    node.build()
  }

  def parseFieldType(js: Json): TyField = {
    js.foldWith(new Json.Folder[TyField] {

      override def onString(value: String): TyField =
        TyFieldBuilder().value(jsonSchemaCommon.decodeOrThrow(value)).build()

      override def onArray(value: Vector[Json]): TyField =
        throw ParseCodeException("Field should not be array", null)

      override def onObject(value: JsonObject): TyField = {
        val field = if (value("name").isDefined && value("type").isDefined) {
          val name = getString(value, "name")
          val ty = jsonSchemaCommon.decodeOrThrow(getString(value, "type"))
          TyFieldBuilder().name(name).value(ty).build()
        } else if (value.size == 1) {
          val name = value.keys.head
          val ty = jsonSchemaCommon.decodeOrThrow(getString(value, name))
          TyFieldBuilder().name(name).value(ty).build()
        } else {
          throw ParseCodeException(
            "FieldType must be either: has fields `name` and `type`, has the form of `name: type`. Got " + value
          )
        }
        // TODO: handle list and object recursively

        field
      }

      override def onNull: TyField =
        throw ParseCodeException("Field should not be null")

      override def onBoolean(value: Boolean): TyField =
        throw ParseCodeException("Field should not be boolean")

      override def onNumber(value: JsonNumber): TyField =
        throw ParseCodeException("Field should not be number")
    })
  }

  def parse(json: Json): TyNode = {
    json.foldWith(new Folder[TyNode] {
      override def onNull: TyNode = ???

      override def onBoolean(value: Boolean): TyNode = ???

      override def onNumber(value: JsonNumber): TyNode = ???

      // extension
      override def onString(value: String): TyNode =
        jsonSchemaCommon.decodeOrThrow(value)

      override def onArray(value: Vector[Json]): TyNode = ???

      override def onObject(value: JsonObject): TyNode = {
        if (value("type").exists(_.isArray)) {
          // probably map(parseType) is enough
          TyUnionImpl(getList(value, "type").toList.map(parse))
        } else if (value("anyOf").isDefined) {
          TyUnionImpl(getList(value, "anyOf").toList.map(parse))
        } else if (value("enum").exists(_.isArray)) {

          TyEnumBuilder()
            .variants(
              getList(value, "enum")
                .map(_.asString.get)
                .map(x =>
                  TyVariantBuilder()
                    .name(x)
                    .code(
                      if (options.extendedGrammar && value("number").isDefined)
                        Some(value("number").get.asNumber.get.toInt.get)
                      else None
                    )
                    .build()
                )
                .toList
            )
            .value(
              value("int_enum")
                .map(x =>
                  if (options.extendedGrammar && x.asBoolean.get)
                    TyIntegerImpl(Some(BitSize.B8), Some(true))
                  else Types.string()
                )
                .getOrElse(Types.string())
            )
            .build()
        } else {
          getString(value, "type") match {
            case "object" => parseStruct(value)
            case "array" if options.extendedGrammar =>
              Types.list(value("items").map(parse).getOrElse(Types.any()))
            case "array" =>
              val items = getObject(value, "items")
              Types.list(parse(Json.fromJsonObject(items)))
//            case "function" if options.extendedGrammar => parseFunction(value)
            case ty => jsonSchemaCommon.decodeOrThrow(ty)
          }

        }

      }
    })
  }

}
