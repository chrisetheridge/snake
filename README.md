# snake

[![Clojars Project](https://img.shields.io/clojars/v/snake.svg)](https://clojars.org/snake)

A simple library to access Amazon S3 from Clojure.



## Description

`snake` allows you to access Amazon S3 buckets in Clojure code. This allows you to do the following, from a Clojure namespace:

- Create / list / delete buckets.
- Create / list / delete objects.
- More in the future.


## Usage

First, add `snake` to your `project.clj`:

`[snake "0.7.1"]`

`snake`, by default, tries to use the **instance profiles** set in `~/.aws/credentials`. However, if you have setup snake already with `snake/setup!`, then `snake` will use these details instead.

You can read more about instance profiles [here](http://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html).

### Copying files

Copy one object from a source bucket to a destination bucket. By default, if a second bucket is not given, then `snake` will just copy to the source bucket.

```clj
(snake/copy src-bucket src-key dest-bucket dest-key)
```

### Uploading files

Upload a file to a bucket (basic way). `snake` will infer the content type of the file, when using the basic way of uploading files.

```clj
(snake/upload! bucket filename file)
```

`upload!` calls `put-object`, which is also public.

```clj
(snake/put-object bucket key file)
```

To specify the content type of the file:

```clj
(snake/put-object-with-content-type bucket filename content-type file)
```

If you would like to get the content type of the file, you can do the following.

```clj
(snake/filename->content-type filename)
```

### Miscellaneous

Listing objects.

```clj
(snake/list-objects bucket {:prefix "foo" :delimeter "foo-bar/"})
```

Return a unique name for a filename.

```clj
(snake/unique-key filename)
```

Get the URL for a bucket and filename.

```clj
(snake/url-for-bucket-and-key bucket file)
```

## License

Take a look at the `LICENSE` file in the root of this repository.
