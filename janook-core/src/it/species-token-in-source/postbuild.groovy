def log = new File(basedir, 'build.log').text

assert log.contains('janook-core carries no species knowledge') :
        'The scan failed without naming the architectural rule that was broken.'
assert log.contains('becomes a rewrite') :
        'The failure did not explain why the rule exists.'
assert log.contains('SpeciesAware.java:6') :
        'The failure did not name the file and line. Scanning without a location makes the reader hunt.'
assert log.contains('janook:allow-species reason=') :
        'The failure did not tell the reader how to suppress a false positive deliberately.'
