import scala.language.experimental.macros

package object records {

  /**
   * Converts a `record` into a case class of type `To`.
   *
   * Case class fields must be a subset of record fields and must conform to
   * record field types.
   *
   * @tparam To case class type.
   * @param record Record to convert
   */
  def convertRecord[To](record: R): To = macro records.RecordConversions.convertRecord_impl[To]

}
