# TestApp
test app for check all kind of things in android java and kotlin

## 1. check garbage collector behavior in java for circular refernce

the dependncis are: C -> B , B -> C, MainActity -> B

- only after we remove the reference for b from the main activity the classes B, C are freed by the garbage collector
- the fact that there is circular reference between B and C don't prevent the garbage collector to collet B and C objects

