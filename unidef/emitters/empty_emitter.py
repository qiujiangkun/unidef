from pydantic import BaseModel

from unidef.emitters import Emitter
from unidef.models import config_model
from unidef.models.config_model import ModelDefinition
from unidef.languages.common.type_model import Traits, DyType
from unidef.utils.formatter import *
from unidef.utils.typing import List


class EmptyEmitter(Emitter):
    def accept(self, s: str) -> bool:
        return s == "no_target"

    def emit_model(self, target: str, model: ModelDefinition) -> str:
        model.get_parsed()
        return ""

    def emit_type(self, target: str, ty: DyType) -> str:
        return ""

