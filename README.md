# snake

A simple library to access Amazon S3 from Clojure.



## Description

`snake` allows you to access Amazon S3 buckets in Clojure code. This allows you to do the following, from a Clojure namespace:

- Create / list / delete buckets.
- Create / list / delete objects.
- More in the future.



## Usage

First, add `snake` to your `project.clj`:

`[snake "X.X.X"]`

Where, `"X.X.X"` is the current version.

`snake`, by default, tries to use the **instance profiles** set in `~/.aws/credentials`. If it cannot find these, it will fallback to environment variables. The environment variables can either be set, or the following ones will be used:

- Access key: `ENV_CONFIG_S3_ACCESS_KEY`
- Secret key: `ENV_CONFIG_S3_SECRET_KEY`



More instructions to be written.



## License

Take a look at the `LICENSE` file in the root of this repository.