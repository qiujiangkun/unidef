package unidef.languages.python

import unidef.languages.common.*
import unidef.utils.{CodeGen, CodegenException, ParseCodeException}

import java.time.LocalDateTime
import scala.jdk.CollectionConverters.*

private case class PythonCodeGenField(
    name: String,
    orig_name: String,
    ty: String
)
class PythonCodeGen(naming: NamingConvention = PythonNamingConvention) extends KeywordProvider {

  protected val TEMPLATE_ENUM_CODEGEN: String =
    """|class $name($enum_type):
       |#foreach ($field in $fields)
       |    $field.name() = $field.orig_name()
       |#end
       |""".stripMargin

  def generateEnum(enm: TyEnum, importManager: Option[ImportManager] = None): String = {
    importManager.foreach(_ += AstImport("enum"))
    val context = CodeGen.createContext
    context.put("name", naming.toClassName(enm.getName.get))
    enm.getContentValue.getOrElse(TyString) match {
      case TyString => context.put("enum_type", "str, enum.Enum")
      case _: TyInteger => context.put("enum_type", "enum.IntEnum")
    }
    var counter = -1
    context.put(
      "fields",
      enm.variants
        .map(x => x.names.head -> x.code)
        .map { (name, code) =>
          counter += 1
          PythonCodeGenField(
            naming.toEnumKeyName(name),
            enm.getContentValue match {
              case Some(x @ TyString) => s"'${name}'"
              case Some(_: TyInteger) => s"${code.getOrElse(counter)}"
              case None => s"'${naming.toEnumValueName(name)}'"
              case Some(t) =>
                throw CodegenException(s"Does not support ${t} as enum value type")
            },
            null
          )
        }
        .asJava
    )

    CodeGen.render(TEMPLATE_ENUM_CODEGEN, context)
  }
  def generateStatement(body: AstNode): String = {
    body match {
      case AstRawCode(raw) => raw
      case _ => ???
    }
  }
  def generateImports(imports: Seq[AstImport]): String = {
    imports
      .map {
        case AstImportSingle(paths) => "import " + paths.mkString(".")
        case AstImportMulti(path, objs) =>
          "from " + path.mkString(".") + " import " + objs.mkString(", ")
        case AstImportAs(path, obj, as) =>
          "from " + path.mkString(".") + " import " + obj + " as " + as
        case AstImportRaw(imports) =>
          imports
        case _ => ???
      }
      .mkString("\n")
  }
  protected val TEMPLATE_FILE_CODEGEN: String =
    """|# generated by unidef
       |$imports
       |#foreach($stmt in $block)
       |$stmt
       |#end
       |""".stripMargin
  def generateFile(source: AstSourceFile): String = {
    val context = CodeGen.createContext
    context.put("time", LocalDateTime.now())
    context.put(
      "imports",
      generateImports(source.imports)
    )
    context.put("block", source.body.map(generateStatement).asJava)
    CodeGen.render(TEMPLATE_FILE_CODEGEN, context)
  }

}
