# edamame

The eda-maps-memoizer is a simple in-memory holder for nested structures of the JSON kind. This kind of data is
represented as maps and lists of maps and lists, with primitive leaves.

It is intended for cases where we are dealing with many objects of the same type, with considerable shared data.
edamame identifies and canonicalizes shared values and substructures, enabling a larger number of objects in memory.

To use it, assign a unique id to you map and store it under that id. Then use the same id to look it up later.

That's really it. Behind the scenes, all common leaves and substructures have been identified and stored exactly once.
(Except in rare cases of a hash collisions.)

## Implementation Notes

### Supported data forms

edamame works on generic trees in the form of nested Java maps (`Map<?, ?>`) and lists (`Iterable<?>`) with
`String`-like keys and primitive leaves. This is what a sensible JSON library will be able to produce.

Leaf nodes should be primitives – strings, numbers, BigDecimals, BigIntegers, booleans – but they can really be
anything that implements equals/hashCode. If you put your rich domain model POJOs in there, edamame will use their
`hashCode()`.

In addition, each object needs to have a unique key, or identifier.

### Caveats

As one would expect, edamame performs hashing of nodes to identify duplicate substructures. We use MD5 for this. It may
not be the cryptographic bee's knees any more, but 128 bits will likely keep your data apart. If one day they don't,
edamame will detect it and fall back to a simple key-value strategy for any data that collides.

For increased efficiency, inputs will be normalized so that functionally equivalent structures can be detected. Empty
maps and lists will be omitted, as well as null leaves. Also, the order of keys in maps is ignored.

However, the order of items in lists will be preserved, and nulls in lists will be respected, as these carry meaning.

### Immutability

Data retrieved from the memoizer is immutable.__