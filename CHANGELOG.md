# Change Log

## [0.7.0] [ BREAKING! ]

- Removed `content-type` argument from from `put-object!`. The AWS SDK can handle the content type of the uploaded file. It is better to rely on their implementation, than use our own implementation.
- `upload!` arguments remaind unchanged, but it no longer generates and sets the content type of the uploaded file.
- `put-object-with-content-type` has been added if you need the ability to specify the type.
- `get-object` no longer longer gets the `ObjectContent`.
- `get-object-content` has been added to get an object, and then the content of the object.

## [0.6.9]
- New method `get-object-input-stream` which returns the raw object input stream,
  instead of a string.
- Bump AWS SDK to `1.11.215` (Updated 18 October 2017)
- Remove `joda-time` exclusion so the library can be used by projects
  that don't include it.

### Fixed
- Clojure 1.9 compatibility
## [0.6.8]
### Fixed
- Clojure 1.9 compatibility

## [0.6.7]
### Added
- HTML support for content types
