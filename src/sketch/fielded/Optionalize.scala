package sketch.fielded

// Makes something optional if it isn't yet
type Optionalize[T] = T match {
  case Option[x] => T
  case _         => Option[T]
}

// Drops Option if present
type UnOption[T] = T match {
  case Option[x] => x
  case _         => T
}
