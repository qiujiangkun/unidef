package unidef.languages.python

import unidef.common.{KeywordProvider, NamingConvention}
import unidef.common.ty.*
import unidef.common.ast.{AstImport, AstImportRaw, AstImportSimple, AstNode, AstRawCode, AstSourceFile, ImportManager}
import unidef.utils.{ CodegenException, ParseCodeException}

import java.time.LocalDateTime
import scala.jdk.CollectionConverters.*

private case class PythonCodeGenField(
    name: String,
    orig_name: String,
    ty: String
)
class PythonCodeGen(naming: NamingConvention = PythonNamingConvention) extends KeywordProvider {
  def renderEnum(name: String, enum_type: Option[String], fields: List[PythonCodeGenField]): String =
    s"class $name" + enum_type.map(x => s"($x)").getOrElse("") + ":\n"
    + fields.map(f => s"    ${f.name} = ${f.orig_name}").mkString("\n")
    + "\n    pass"


  def generateEnum(enm: TyEnum, importManager: Option[ImportManager] = None): String = {
    importManager.foreach(_ += AstImport("enum"))
    val name = naming.toClassName(enm.getName.get)
    val enum_type = enm.getValue.getOrElse(TyStringImpl()) match {
      case _: TyString => "str, enum.Enum"
      case _: TyInteger => "enum.IntEnum"
    }
    var counter = -1
    val fields =
      enm.variants
        .map(x => x.names.head -> x.code)
        .map { (name, code) =>
          counter += 1
          PythonCodeGenField(
            naming.toEnumKeyName(name),
            enm.getValue match {
              case Some(x: TyString) => s"'$name'"
              case Some(_: TyInteger) => s"${code.getOrElse(counter)}"
              case None => s"'${naming.toEnumValueName(name)}'"
              case Some(t) =>
                throw CodegenException(s"Does not support $t as enum value type")
            },
            null
          )
        }


    renderEnum(name, Some(enum_type), fields)
  }
  def generateStatement(body: AstNode): String = {
    body match {
      case x : AstRawCode => x.getCode.get
      case _ => ???
    }
  }
  def generateImports(imports: Seq[AstImport]): String = {
    imports
      .map {
        case AstImportSimple(obj :+ x, y +: Nil) if obj.nonEmpty && x == y =>
          s"from ${obj.mkString(".")} import $x"
        case AstImportSimple(obj, use) if obj == use => s"import ${obj.mkString(".")}"
        case AstImportSimple(obj, use) => s"import ${obj.mkString(".")} as ${use.mkString(".")}"
        case AstImportRaw(imports) =>
          imports
        case _ => ???
      }
      .mkString("\n")
  }
  def renderFile(imports: String, stmt: List[String]): String =
    s"""# generated by unidef
    |$imports
    |${stmt.mkString("\n")}
    """.stripMargin


  def generateFile(source: AstSourceFile): String = {
    val imports = generateImports(source.imports)

    val stmt = source.body.map(generateStatement)
    renderFile(imports, stmt)
  }

}
