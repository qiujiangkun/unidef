package com.jeekrs.unidef
package languages.common

import utils.ExtKey

trait GetExtKeys {
  def keysOnField: List[ExtKey] = List()
  def keysOnDecl: List[ExtKey] = List()
}
