package utils

import java.nio.charset.StandardCharsets
import java.util.Base64
object AppUtils {

  def encodeStr (str : String) : String = {
    val bytes = str.getBytes(StandardCharsets.UTF_8)
    Base64.getEncoder().encodeToString(bytes)
  }

  def decodeStr(str: String): String = {
    val decodedBytes = Base64.getDecoder().decode(str)
    new String(decodedBytes, StandardCharsets.UTF_8)
  }

}
