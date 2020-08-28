package lit.fass.server.security

/**
 * @author Michael Mair
 */
@Deprecated
object Role extends Enumeration {
  type Role = Value
  val
  ADMIN,
  READER,
  WRITER,
  EXECUTER
  = Value
}
