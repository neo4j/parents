# Parent POMs for Neo4j and Friends

## What?

The Neo4j Maven project inherits from this parent POM complex. The inheritance includes, but is not limited to, the list of dependencies.

## Why?

By having an external parent POM, embedded users can easily share the same dependencies that Neo4j uses, at the same version.

### But Why??

Well OK, this parent POM complex has grown a lot of cruft over time. Bear with...

## How?

So, to operate the parent POM complex, this is the workflow:

1. Make your changes to POMs etc., then commit+push them
2. Run `mvn release:prepare --activate-profiles sign-artifacts -Darguments="-Dgpg.keyname=redacted -Dgpg.passphrase=redacted" release:perform`
3. Answer the prompts for release version, repo tag, and next-snapshot version
4. Log into oss.sonatype.org and close+release the resulting staging repository
5. Push the commits the release plugin made - the tag is pushed automagically
6. Rejoice.

### Caveats

We use Maven Release Plugin. It is terrible. Here are some things you might need to solve, and the plugin is not helpful in the error messages it gives.

Firstly, there is a _sign-artifacts_ profile that triggers GPG signing of the POMs, which is something Maven Central demands. It is a profile because then it only happens when you ask for it, and not all the time.

* If you happen to have messed up your local GPG keystore somehow, beware: the error message from GPG plugin are not easy to decode. Best to try with GPG manually to check it works: `gpg --sign ...` something and `gpg --verify ...` the
signature using the desired GPG key and passphrase that you intend to use for the real deal.
* Secondly, depending on your passphrase you _might_ have to escape stuff on the command line to get it safely through to the eventual call out to `mvn verify` that does the actual invokation of GPG plugin and therefore the actual signing

Next, getting access to Nexus over at Sonatype: Maven Deploy Plugin does not support passing in credentials on the command line. Yes you read that right. So here is a workaround.

* Deploy plugin is going to look in your `settings.xml`, which normally lives in ``~/.m2/settings.xml`
* It is going to look for a server entry with id _sonatype-nexus-staging_ for credentials

Other than that, happy hacking!
