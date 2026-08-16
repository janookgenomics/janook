// The build must fail *for the right reason*. Asserting only that it failed would pass just as
// happily on a typo in the POM.
def log = new File(basedir, 'build.log').text

assert log.contains('janook-core must have no third-party dependencies') :
        'The enforcer failed without naming the architectural rule that was broken.'
assert log.contains('embedded in someone else') :
        'The failure did not explain why the rule exists. A rule nobody understands is a rule somebody deletes.'
assert log.contains('junit-jupiter') :
        'The failure did not name the offending artifact.'
