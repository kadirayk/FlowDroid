

# FlowDroid with SparseBoomerang

This is a [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) fork that can work with [SparseBoomerang](https://github.com/secure-software-engineering/SparseBoomerang) as alias analysis provider.  
SparseBoomerang provides 3 alias analysis variants that can be activated using `-aa OPTIONNAME` in FlowDroid's command line arguments.
- `BOOMERANG`: Default Boomerang
- `TYPEBASEDSPARSEBOOMERANG`: Alias analysis using Type-aware sparsification
- `ALIASAWARESPARSEBOOMERANG`: Alias analysis using Alias-aware sparsification


## Publications
This work was used in the evaluations of the SparseBoomerang paper.  
Preprint is available: [Two Sparsification Strategies for Accelerating Demand-Driven Pointer Analysis](https://www.bodden.de/pubs/kb23sparsification.pdf) (ICST 2023)
