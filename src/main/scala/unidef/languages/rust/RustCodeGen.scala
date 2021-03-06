package unidef.languages.rust

import unidef.common.NamingConvention
import unidef.common.ty.*
import unidef.common.ast.*
import unidef.utils.{TextTool, TypeEncodeException}


case class RustCodeGen(naming: NamingConvention) {
  val common = RustCommon()

  def renderMethod(
                    override_a: String,
                    name: String,
                    params: String,
                    ret: Option[String],
                    body: Option[String]
                  ): String = {
    s"fn $name$params" +
      ret.map(r => s" -> $r").getOrElse("") +
      body.map(b => s" {\n${TextTool.indent_hard(b, 2)}\n}").getOrElse("")
  }

  def generateMethod(method: AstFunctionDecl): String = {
    val name = naming.toMethodName(method.name)
    // TODO: support multiple param lists
    val parameters = Asts.flattenParameters(method.parameters)
    val params =
      if (parameters.isEmpty && method.name.startsWith("get"))
        ""
      else
        "(" + parameters
          .map(x => x.name + ": " + common.encodeOrThrow(x.ty, "param"))
          .mkString(", ") + ")"

    val body = method.body.map(generate)
    val override_a = if (method.overwrite.getOrElse(false)) {
      "override "
    } else {
      ""
    }
    val ret = common.encode(method.returnType)
    renderMethod(override_a, name, params, ret, body)
  }

  def renderClass(
                   cls: String,
                   name: String,
                   params: List[String],
                   fields: List[String],
                   derive: List[String],
                   methods: List[String]
                 ) = {
    val params_a = params.mkString("(", ", ", ")")
    val derive_a = if (derive.isEmpty) {
      ""
    } else {
      s" extends ${derive.mkString(" with ")}"
    }
    val body_a =
      if (fields.isEmpty && methods.isEmpty) ""
      else
        "{\n" +
          (fields ++ methods).map(TextTool.indent_hard(_, 2)).mkString("\n") +
          "\n}"

    s"$cls $name$params_a$derive_a $body_a"
  }

  def generateClass(c: AstClassDecl): String = {
    val cls = c.classType.getOrElse("case class")

    def mapParam(x: AstValDef): String = {
      val modifier = if (cls == "case class") {
        ""
      } else if (x.mutability.contains(true)) {
        "var "
      } else {
        "val "
      }
      val default = x.value
        .map { case x: AstRawCode =>
          " = " + x.code
        }
        .getOrElse("")
      modifier + x.name + ": " + common
        .encode(x.ty)
        .getOrElse(throw TypeEncodeException("Scala", x.ty))
        + default
    }

    val name = naming.toClassName(c.name)
    val params = Asts.flattenParameters(c.parameters).map(mapParam)
    val fields = c.fields.map(mapParam)
    val derive = c.derives.map(_.asInstanceOf[AstIdent]).map(_.name).map(naming.toClassName)
    val methods = c.methods.map {
      case x: AstFunctionDecl => generateMethod(x)
      case x: AstRawCode => x.code
    }
    renderClass(cls, name, params, fields, derive, methods)
  }

  def renderEnum(name: String, variants: List[String]): String = {
    s"sealed trait $name\n"
      + variants.map(x => s"case object $x extends $name").mkString("\n")
  }

  def generateScala2Enum(enm: TyEnum): String = {
    val name = TextTool.toPascalCase(enm.name.get)
    val variants = enm.variants.map(x => x.names.head).map(TextTool.toScreamingSnakeCase)
    renderEnum(name, variants)
  }

  def generateRaw(code: AstRawCode): String = {
    code.code
  }


  def generate(n: AstNode): String = {
    n match {
      case x: AstDecls =>
        x.decls.map(generate).mkString("\n")
      case x: AstBlock =>
        x.stmts.map(generate).mkString(";\n")
      case x: AstApply =>
        generate(x.applicant) + x.arguments.argumentListsContent.map(y => y.argumentListContent.map(generate).mkString("(", ", ", ")")).mkString("")
      case x: AstFunctionDecl =>
        generateMethod(x)
      case n: AstIdent =>
        n.name
      case n: AstLiteralString =>
        n.literalString
      case n: AstLiteralInt =>
        n.literalInt.toString
      case n: AstLiteralUnit =>
        "()"
      case n: AstLiteralNone =>
        "None"
      case n: AstLiteralNull =>
        "ptr::null()"
      case n: AstRawCode =>
        n.code

      case a: AstArgument =>
        generate(a.value.get)

    }
  }
}
