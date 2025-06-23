# edamame

The eda-maps-memoizer is a simple in-memory holder for JSON-like data. JSON-like means the data is represented as
maps and lists of maps and lists, with primitive leaves.

It is intended for cases where we are dealing with many objects of the same type, with lots of shared data. edamame
takes advantage of shared substructures to save memory, and can thus hold a larger number of objects.

## Implementation Notes

### Data forms

Data in this context means generic trees in the form of nested Java maps and lists (`Map<?, ?>`, `Iterable<?>` ) with
`String`-like keys and primitive leaves – just like a sensible JSON library will be able to produce.

### Strategy

As one would expect, edamame performs hashing of nodes to identify duplicate substructures. We use MD5 for this – it may
not be the cryptographical bee's knees any more, but 128 bits will likely keep your data part. If they don't, edamame
detects it and falls back to a simple key-value strategy for any data that collides.

## Caveats

### Data forms

Leaf nodes should be primitives – strings, numbers, BigDecimals, BigIntegers, booleans – but they can really be
anything that implements equals/hashCode. If you put your rich domain model POJOs in there, edamame will use their
`hashCode()`.

### Immutability

As you can (and should) imagine, this assumes that the data are not mutated after insertion.
If that happens, there are no guarantees. It _is_ asking for trouble, but not even that can be guaranteed.

If you didn't imagine that (or didn't even go "ah" just now), then please don't use this library.