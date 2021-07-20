# clj-cookbook

A collection of useful Clojure recipes (the sort of things you do once in the lifetime of a project and then forget how to do).

Meta level recipes will be documented here.

## Formatting pre-commit hook

Pre-commit hook bash script can be found [here](https://github.com/andersmurphy/clj-cookbook/blob/master/git-hooks/pre-commit).

Downloads and caches [zprint](https://github.com/kkinnear/zprint) and uses it to format staged code automatically on commit.

Formatting rules can be configured [here](https://github.com/andersmurphy/clj-cookbook/blob/master/.zprintrc).

Setup:

```sh
git config core.hooksPath git-hooks
chmod +x git-hooks/pre-commit
```

The accompanying blog post can be found [here](https://andersmurphy.com/2020/08/16/clojure-code-formatting-pre-commit-hook-with-zprint.html).
