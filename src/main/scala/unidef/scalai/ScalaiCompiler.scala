package unidef.scalai
import scala.sys.process.Process
import unidef.languages.common.{AstNode, AstUnit}
import unidef.utils.FileUtils

import java.io.{File, FileWriter}
import scala.io.Source
import scala.quoted.{Expr, Quotes}
import scala.tasty.inspector.TastyInspector

class ScalaiCompiler {
  def compileOnly(code: String): File = {
    val path = File.createTempFile("scalai_", ".scala")
    val writer = FileWriter(path)
    writer.write(code)
    writer.close()
    Process(Seq("scala3-compiler", path.getAbsolutePath), path.getParentFile).!!
    path
  }
  def compileAndLift(code: String): AstNode = {
    if (code.isEmpty) {
      return AstUnit
    }
    val path = compileOnly(code)
    val tasty = path.getAbsolutePath.replace(".scala", "$package.tasty")
    val tastyFiles = List(tasty)
    val lifter = ScalaiTastyHelper()
    TastyInspector.inspectTastyFiles(tastyFiles)(lifter)
    lifter.getAstNode
  }
  transparent inline def lift[T](inline code: T): AstNode = {
    ${
      liftQuotedImpl('code)
    }
  }

  transparent inline def liftAndUnlift[T](inline code: T): T = {
    ${
      liftAndUnliftImpl('code)
    }
  }
}

object ScalaiCompiler {
  def main(args: Array[String]): Unit =
    val source = FileUtils.readFile("examples/scala_parser.scala")
    val compiler = ScalaiCompiler()
    val compiled = compiler.compileAndLift(source)
    println(compiled)
}
