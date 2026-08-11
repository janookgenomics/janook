def log = new File(basedir, 'build.log').text

assert log.contains('arrow points one way only') :
        'The enforcer failed without naming the architectural rule that was broken.'
assert log.contains('the dependency is inverted') :
        'The failure did not explain how to resolve the situation properly.'
assert log.contains('janook-cli') :
        'The failure did not name the offending module.'
