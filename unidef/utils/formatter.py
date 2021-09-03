from unidef.utils.typing_compat import *
from pydantic import BaseModel
from beartype import beartype


class Formatter:
    def to_string(self):
        pass


class IndentedWriter(Formatter, BaseModel):
    indent: int = 0
    tab: str = '    '
    content: List[str] = []

    def try_indent(self):
        try:
            if len(self.content) == 0 or self.content[-1] == '\n':
                self.content.append(self.tab * self.indent)
        except:
            pass

    @beartype
    def append_line(self, s: str = ''):
        if s:
            self.try_indent()
            self.content.extend([s, '\n'])
        else:
            self.content.append('\n')

    @beartype
    def append(self, s: str):
        self.try_indent()
        self.content.append(s)

    def incr_indent(self, level=1):
        self.indent += level

    def decr_indent(self, level=1):
        self.indent -= level

    def to_string(self, strip_left=False):
        if strip_left and self.content[0].isspace():
            return ''.join(self.content[1:])
        else:
            return ''.join(self.content)

    def clone(self):
        writer = IndentedWriter()
        writer.indent = self.indent
        writer.content = self.content[:]
        writer.tab = self.tab
        return writer


class Formatee:
    def format_with(self, formatter: Formatter):
        pass


class Braces(Formatee):
    def __init__(self, val: Formatee, open='{', close='}', new_line=True):
        self.value = val
        self.open = open
        self.close = close
        self.new_line = new_line

    def format_with(self, writer: IndentedWriter):
        if self.new_line:
            writer.append_line(self.open)
            writer.incr_indent()
        else:
            writer.append(self.open)

        self.value.format_with(writer)

        if self.new_line:
            writer.decr_indent()
            writer.append_line(self.close)
        else:
            writer.append(self.close)


class IndentBlock(Braces):
    def __init__(self, val: Formatee):
        super().__init__(val, open=':', close='')


class Function(Formatee):
    def __init__(self, func):
        self.func = func

    def format_with(self, writer: IndentedWriter):
        self.func(writer)


class Text(Formatee):
    def __init__(self, text):
        self.text = text

    def format_with(self, writer: IndentedWriter):
        writer.append(self.text)