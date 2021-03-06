import copy

from unidef.utils.transformer import *
from unidef.utils.typing_ext import *
from unidef.utils.visitor import VisitorPattern
from unidef.utils.template import Code
from typedmodel import BaseModel

@abstract
class SourceNode(BaseModel):
    pass


class IndentNode(SourceNode):
    level: int


class DedentNode(SourceNode):
    level: int


class TextNode(SourceNode):
    text: str

    def __init__(self, text: str, **data: Any):
        super().__init__(text=text, **data)


class BulkNode(SourceNode):
    sources: List[SourceNode] = []

    def __init__(self, sources, **data: Any):
        super().__init__(sources=sources, **data)


class BracesNode(SourceNode):
    open: str = "{"
    value: SourceNode
    close: str = "}"
    new_line: bool = True
    post_new_line: bool = True


class LineNode(SourceNode):
    content: SourceNode

    def __init__(self, content, **data: Any):
        super().__init__(content=content, **data)


class CodeNode(SourceNode):
    code: Code


class StructuredFormatter(NodeTransformer[SourceNode, str], VisitorPattern):
    indent: int = 0
    tab: str = "    "
    indented: bool = False
    nodes: List[SourceNode] = []
    collection: List[str] = []
    functions: Optional[List[NodeTransformer]] = None

    @beartype
    def accept(self, node: Input) -> bool:
        return True

    @beartype
    def transform(self, node: Input) -> Output:
        self.nodes = [node]
        return self.to_string()

    @beartype
    def append_format_node(self, node: SourceNode):
        self.nodes.append(node)

    @beartype
    def format_indent_node(self, node: IndentNode):
        self.indent += node.level

    @beartype
    def format_dedent_node(self, node: DedentNode):
        self.indent -= node.level

    @beartype
    def format_text_node(self, node: TextNode):
        self._try_indent()
        self.collection.append(node.text)

    @beartype
    def format_line_node(self, node: LineNode):
        self._try_indent()
        self.format_node(node.content)
        self._line_break()

    @beartype
    def format_bulk_node(self, node: BulkNode):
        if len(node.sources) > 0:
            self._try_indent()
            for n in node.sources:
                self.format_node(n)

    @beartype
    def format_braces_node(self, node: BracesNode):
        if node.new_line:
            self._try_indent()
            self.collection.append(node.open)
            self._line_break()
            self._incr_indent()
        else:
            self.collection.append(node.open)

        self.format_node(node.value)

        if node.new_line:
            self._decr_indent()
            self._try_indent()
            self.collection.append(node.close)
            if node.post_new_line:
                self._line_break()
        else:
            self.collection.append(node.close)

    @beartype
    def format_code_node(self, node: CodeNode):
        self.collection.append(node.code.render())

    @beartype
    def format_node(self, node: SourceNode):
        if self.functions is None:
            self.functions = self.get_functions("format_")
        for func in self.functions:
            if func.accept(node):
                func.transform(node)
                break
        else:
            raise Exception("No function to format node " + type(node).__name__)

    def to_string(self, strip_left=False):
        self.collection = []
        for n in self.nodes:
            self.format_node(n)
        if strip_left and self.collection[0].isspace():
            return "".join(self.collection[1:])
        else:
            return "".join(self.collection)

    def copy(self, **kwargs) -> __qualname__:
        return copy.deepcopy(self)

    def _try_indent(self):
        if not self.indented:
            self.collection.append(self.tab * self.indent)
            self.indented = True

    def _incr_indent(self, level=1):
        self.indent += level
        self.indented = False

    def _decr_indent(self, level=1):
        self.indent -= level
        self.indented = False

    def _line_break(self):
        self.collection.append("\n")
        self.indented = False
