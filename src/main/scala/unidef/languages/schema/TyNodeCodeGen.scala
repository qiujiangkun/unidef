package unidef.languages.schema

import com.typesafe.scalalogging.Logger
import io.circe.yaml.parser
import io.circe.{Json, JsonNumber, JsonObject}
import unidef.common.{NoopNamingConvention, ast}
import unidef.languages.javascript.{JsonSchemaCommon, JsonSchemaParser}
import unidef.languages.scala.{ScalaCodeGen, ScalaCommon}
import unidef.utils.FileUtils.readFile
import unidef.utils.{ParseCodeException, TextTool, TypeDecodeException, TypeEncodeException}
import unidef.common.ty.*
import unidef.common.ast.*

import java.io.PrintWriter
import scala.collection.mutable

case class TyNodeCodeGen() {
  val logger: Logger = Logger[this.type]
  val common = JsonSchemaCommon(true)

  def collectFields(ty: Type): Set[TyField] = {
    ty.fields.toSet
  }
  def collectFields(types: List[Type]): Set[TyField] = {
    types.flatMap(collectFields).toSet
  }
  def scalaField(name: String, derive: String, methods: List[String]): AstClassDecl = {
    AstClassDecl(
      name,
      Nil,
      Nil,
      methods.map(x => AstRawCodeImpl(x, None)),
      List(AstClassIdent(derive))
    )
  }
  def tryWrapValue(x: TyField): TyNode = if (x.defaultNone.get) TyOptionalImpl(x.value) else x.value

  def generateScalaKeyObject(field: TyField): AstClassDecl = {
    val traitName = "Key" + TextTool.toPascalCase(field.name)
    val cls = field.value match {
      case _: TyInteger =>
        scalaField(traitName, "KeywordInt", Nil)
      case _: TyString => scalaField(traitName, "KeywordString", Nil)
      case _: TyBoolean => scalaField(traitName, "KeywordBoolean", Nil)
      case _ =>
        val scalaCommon = ScalaCommon()
        val valueType =
          scalaCommon.encodeOrThrow(field.value, "scala")
        scalaField(traitName, "Keyword", List(s"override type V = ${valueType}"))
    }
    cls.setValue(KeyClassType, "case object")
  }

  def generateScalaHasTrait(field: TyField): AstClassDecl = {
    val traitName = "Has" + TextTool.toPascalCase(field.name)
    val scalaCommon = ScalaCommon()
    val valueType =
      scalaCommon.encodeOrThrow(tryWrapValue(field), "Scala")

    scalaField(
      traitName,
      "TyNode",
      if (field.defaultNone.get) List(s"def get${TextTool.toPascalCase(field.name)}: ${valueType}")
      else List(s"def get${TextTool.toPascalCase(field.name)}: ${valueType}")
    ).setValue(KeyClassType, "trait")
  }
  def generateScalaCompoundTrait(ty: Type): AstClassDecl = {
    val scalaCommon = ScalaCommon()
    val fields = ty.fields.toList

    AstClassDecl(
      "Ty" + TextTool.toPascalCase(ty.name),
      Nil,
      Nil,
      fields
        .map(x =>
          AstFunctionDecl(
            "get" + TextTool.toPascalCase(x.name),
            Nil,
            tryWrapValue(x)
          )
        )
        .toList,
      List(AstClassIdent("TyNode"))
        :::
          ty.equivalent
            .flatMap {
              case TyNamed(x) => Some("Ty" + TextTool.toPascalCase(x))
              case _ => None // TODO: support other IS
            }
            .map(x => AstClassIdent(x))
            .toList
          :::
          fields
            .map(x => x.name -> x.value)
            .map((k, v) => "Has" + TextTool.toPascalCase(k))
            .map(x => AstClassIdent(x))
            .toList
    ).setValue(KeyClassType, "trait")
  }
  def generateScalaBuilder(ty: Type): AstClassDecl = {
    val fields = ty.fields
      .map(x => x.copy(name = TextTool.toCamelCase(x.name), value = tryWrapValue(x)))
      .toList
    val codegen = ScalaCodeGen(NoopNamingConvention)
    codegen.generateBuilder(
      "Ty" + TextTool.toPascalCase(ty.name) + "Builder",
      "Ty" + TextTool.toPascalCase(ty.name) + "Impl",
      fields
    )
  }
  def generateScalaCaseClass(ty: Type): AstClassDecl = {
    val fields = ty.fields.map(x => x.copy(name = TextTool.toCamelCase(x.name)))

    AstClassDecl(
      "Ty" + TextTool.toPascalCase(ty.name) + "Impl",
      fields.map(x => AstValDefImpl(x.name, tryWrapValue(x), None, None)).toList,
      Nil,
      fields
        .map(x =>
          AstFunctionDecl(
            "get" + TextTool.toPascalCase(x.name),
            Nil,
            tryWrapValue(x)
          ).setValue(KeyBody, AstRawCodeImpl(x.name, None))
            .setValue(KeyOverride, true)
        ).toList,
      List(AstClassIdent("Ty" + TextTool.toPascalCase(ty.name)))
    ).setValue(KeyClassType, "class")
  }
}
object TyNodeCodeGen {
  def getTypes: Map[String, Type] =
    Seq(
      Type("string"),
      Type("field")
        .field("name", TyStringImpl())
        .field("value", TyNode, required = true),
      Type("list")
        .field("content", TyNode, required = true),
//      Type("enum")
//        .field("variants", TyListImpl(Some(TyStringImpl()))),
      Type("tuple")
        .field("values", TyListImpl(TyNode), required = true),
      Type("optional")
        .field("content", TyNode, required = true),
      Type("result")
        .field("ok", TyNode, required = true)
        .field("err", TyNode, required = true),
      Type("numeric"),
      Type("integer")
        .field("bit_size", TyNamed("bit_size"))
        .field("sized", TyBooleanImpl())
        .is(TyNamed("numeric")),
      Type("real")
        .is(TyNamed("numeric")),
      Type("decimal")
        .field("precision", TyIntegerImpl(None, None))
        .field("scale", TyIntegerImpl(None, None))
        .is(TyNamed("real")),
      Type("float")
        .field("bit_size", TyNamed("bit_size"))
        .is(TyNamed("real")),
      Type("class"),
      Type("struct")
        .field("name", TyStringImpl())
        .field("fields", TyListImpl(TyNamed("TyField")))
        .field("derives", TyListImpl(TyStringImpl()))
        .field("attributes", TyListImpl(TyStringImpl()))
        .field("dataframe", TyBooleanImpl())
        .field("schema", TyStringImpl())
        .is(TyNamed("class"))
        .setCommentable(true),
      Type("object"),
      Type("map")
        .field("key", TyNode, required = true)
        .field("value", TyNode, required = true),
      Type("set")
        .field("content", TyNode, required = true),
      Type("set")
        .field("content", TyNode, required = true)
        .is(TyIntegerImpl(Some(BitSize.B8), Some(false))),
      Type("byte_array")
        .is(TyListImpl(TyIntegerImpl(Some(BitSize.B8), Some(false)))),
      Type("boolean"),
      Type("record"),
      Type("null"),
      Type("char"),
      Type("any"),
      Type("unit"),
      Type("nothing"),
      Type("undefined"),
      Type("inet"),
      Type("uuid")
    )
      .map(x => x.name -> x)
      .toMap

  def main(args: Array[String]): Unit = {
    val types = getTypes.values.toList

    val parser = TyNodeCodeGen()

    println("Parsed types")
    println(types.mkString("\n"))

    val fields = parser.collectFields(types)
    println("Parsed fields")
    println(fields.mkString("\n"))
    val keyObjects = fields.map(parser.generateScalaKeyObject)
    println("Generated key objects")
    println(keyObjects.mkString("\n"))
    val hasTraits = fields.map(parser.generateScalaHasTrait)
    println("Generated has traits")
    println(hasTraits.mkString("\n"))
    val caseClasses = types.map(parser.generateScalaCaseClass)
    println("Generated case classes")
    println(caseClasses.mkString("\n"))
    val compoundTraits = types.map(parser.generateScalaCompoundTrait)
    println("Generated compound traits")
    println(compoundTraits.mkString("\n"))
    val builders = types.map(parser.generateScalaBuilder)
    val scalaCodegen = ScalaCodeGen(NoopNamingConvention)
    val scalaCode =
      (
//        keyObjects.map(scalaCodegen.generateClass)
//        :::
        hasTraits.map(scalaCodegen.generateClass).toList
          ::: caseClasses.map(scalaCodegen.generateClass)
          ::: compoundTraits.map(scalaCodegen.generateClass)
          ::: builders.map(scalaCodegen.generateClass)
      ).mkString("\n")

    println(scalaCode)
    val writer = new PrintWriter("target/TyNodeGen.scala")
    writer.println("""
                     |package unidef.common.ty
                     |
                     |""".trim.stripMargin)
    writer.write(scalaCode)
    writer.close()

  }
}
