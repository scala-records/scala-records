Design decisions about Scala records
====================================

Here we need to decide what operations do we want to support and which operations will go into the first version.

## Modifying Field Values

Should we support modification of record values? If yes we have two possibilities:

1. A generic implementation that will take string literals for field names. Here we have a drawback that auto-completion will not work:

        data.map { r =>
          r.copy("name" -> r.phone.toUppercase, "phone" -> "")
        }

2. A custom method for each record that will have by-name parameters for all fields:

        data.map { r =>
          r.copy(name = r.phone.toUppercase, phone = "")
        }

The question is can we hide the `copy` method in the type signature of the `R` structural type? Possibly, by making `copy` and extension method that is a macro that returns a type with an `apply` method that corresponds to all the fields.

## Join Semantics

Do we need nested records? Do we need joined records?

If yes, for records `r1 = Rec { val x = 1; val y = 2; }` and `r2 = Rec { val y = 3; val z = 4;}` we can:

1. Expose disjoint fields in the joined record and leave common fields in nested records:

        r = Rec {
          val r1 = Rec { val y = 2; };
          val r2 = Rec { val y = 3; };
          val x = 1;
          val z = 4;
        }
   If somebody tries to access `r.y` we can generate a compile time error message that says look in `r2` `r3`.

2. Just leave two nested records (Slick does something similar):

        r = Rec {
          val r1 = Rec { val x = 1; val y = 2; };
          val r2 = Rec { val y = 3; val z = 4; };
        }
   Here we could also have an error message if somebody misses a field.

3. Leave both nested records and provide disjoint fields in the joined record:

        r = Rec {
          val r1 = Rec { val x = 1; val y = 2; };
          val r2 = Rec { val y = 3; val z = 4; };
          val x = 1;
          val z = 4;
        }

## Projection

Do we need projection?

Since we do not have singleton types the only way I see to achieve projections is to say:

    r.project("name", "phone", ...)

## Pattern Matching

Should we allow pattern matching? If yes what would the semantics be?

    case class NamePhone(n: String, phone: String) with Record
 	 val r = Rec {val name = "Tobias"; val phone = "1234567"}
    r match {
      case Rec("name" -> n) => println("?")
    }

    r match {
      case Rec("name" -> n, "phone" -> p) => println("Yes!")
    }

    r match {
      case NamePhone(n, phone) => println("Yes!") // not sure this is possible
    }

I would leave this for future work as it is not quite clear what we need.
