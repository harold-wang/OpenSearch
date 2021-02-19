################################################################################
# This Dockerfile was generated from the template at distribution/src/docker/Dockerfile
#
# Beginning of multi stage Dockerfile
################################################################################
<% /*
  This file is passed through Groovy's SimpleTemplateEngine, so dollars and backslashes
  have to be escaped in order for them to appear in the final Dockerfile. You
  can also comment out blocks, like this one. See:

  https://docs.groovy-lang.org/latest/html/api/groovy/text/SimpleTemplateEngine.html

  We use control-flow tags in this file to conditionally render the content. The
  layout/presentation here has been adjusted so that it looks reasonable when rendered,
  at the slight expense of how it looks here.
*/ %>
################################################################################
# Build stage 0 `builder`:
# Extract Renameme artifact
################################################################################

FROM ${base_image} AS builder
<% if (docker_base == 'ubi') { %>
# Install required packages to extract the Renameme distribution
RUN ${package_manager} install -y tar gzip
<% } %>
# `tini` is a tiny but valid init for containers. This is used to cleanly
# control how ES and any child processes are shut down.
#
# The tini GitHub page gives instructions for verifying the binary using
# gpg, but the keyservers are slow to return the key and this can fail the
# build. Instead, we check the binary against the published checksum.
RUN set -eux ; \\
    tini_bin="" ; \\
    case "\$(arch)" in \\
        aarch64) tini_bin='tini-arm64' ;; \\
        x86_64)  tini_bin='tini-amd64' ;; \\
        *) echo >&2 ; echo >&2 "Unsupported architecture \$(arch)" ; echo >&2 ; exit 1 ;; \\
    esac ; \\
    curl --retry 8 -S -L -O https://github.com/krallin/tini/releases/download/v0.19.0/\${tini_bin} ; \\
    curl --retry 8 -S -L -O https://github.com/krallin/tini/releases/download/v0.19.0/\${tini_bin}.sha256sum ; \\
    sha256sum -c \${tini_bin}.sha256sum ; \\
    rm \${tini_bin}.sha256sum ; \\
    mv \${tini_bin} /tini ; \\
    chmod +x /tini

RUN mkdir /usr/share/renameme
WORKDIR /usr/share/renameme

${source_renameme}

RUN tar zxf /opt/renameme.tar.gz --strip-components=1
RUN sed -i -e 's/ES_DISTRIBUTION_TYPE=tar/ES_DISTRIBUTION_TYPE=docker/' /usr/share/renameme/bin/renameme-env
RUN mkdir -p config config/jvm.options.d data logs
RUN chmod 0775 config config/jvm.options.d data logs
COPY config/renameme.yml config/log4j2.properties config/
RUN chmod 0660 config/renameme.yml config/log4j2.properties

################################################################################
# Build stage 1 (the actual Renameme image):
#
# Copy renameme from stage 0
# Add entrypoint
################################################################################

FROM ${base_image}

ENV ELASTIC_CONTAINER true

RUN for iter in {1..10}; do \\
      ${package_manager} update --setopt=tsflags=nodocs -y && \\
      ${package_manager} install --setopt=tsflags=nodocs -y \\
        nc shadow-utils zip unzip <%= docker_base == 'ubi' ? 'findutils procps-ng' : '' %> && \\
      ${package_manager} clean all && exit_code=0 && break || exit_code=\$? && echo "${package_manager} error: retry \$iter in 10s" && \\
      sleep 10; \\
    done; \\
    (exit \$exit_code)

RUN groupadd -g 1000 renameme && \\
    adduser -u 1000 -g 1000 -G 0 -d /usr/share/renameme renameme && \\
    chmod 0775 /usr/share/renameme && \\
    chown -R 1000:0 /usr/share/renameme

WORKDIR /usr/share/renameme
COPY --from=builder --chown=1000:0 /usr/share/renameme /usr/share/renameme
COPY --from=builder --chown=0:0 /tini /tini

# Replace OpenJDK's built-in CA certificate keystore with the one from the OS
# vendor. The latter is superior in several ways.
# REF: https://github.com/elastic/elasticsearch-docker/issues/171
RUN ln -sf /etc/pki/ca-trust/extracted/java/cacerts /usr/share/renameme/jdk/lib/security/cacerts

ENV PATH /usr/share/renameme/bin:\$PATH

COPY bin/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# The JDK's directories' permissions don't allow `java` to be executed under a different
# group to the default. Fix this.
RUN find /usr/share/renameme/jdk -type d -exec chmod 0755 '{}' \\; && \\
    chmod g=u /etc/passwd && \\
    chmod 0775 /usr/local/bin/docker-entrypoint.sh

# Ensure that there are no files with setuid or setgid, in order to mitigate "stackclash" attacks.
RUN find / -xdev -perm -4000 -exec chmod ug-s {} +

EXPOSE 9200 9300

LABEL org.label-schema.build-date="${build_date}" \\
  org.label-schema.license="${license}" \\
  org.label-schema.name="Renameme" \\
  org.label-schema.schema-version="1.0" \\
  org.label-schema.url="https://www.elastic.co/products/elasticsearch" \\
  org.label-schema.usage="https://www.elastic.co/guide/en/elasticsearch/reference/index.html" \\
  org.label-schema.vcs-ref="${git_revision}" \\
  org.label-schema.vcs-url="https://github.com/elastic/elasticsearch" \\
  org.label-schema.vendor="Elastic" \\
  org.label-schema.version="${version}" \\
  org.opencontainers.image.created="${build_date}" \\
  org.opencontainers.image.documentation="https://www.elastic.co/guide/en/elasticsearch/reference/index.html" \\
  org.opencontainers.image.licenses="${license}" \\
  org.opencontainers.image.revision="${git_revision}" \\
  org.opencontainers.image.source="https://github.com/opendistro-for-elasticsearch/search" \\
  org.opencontainers.image.title="Renameme" \\
  org.opencontainers.image.url="https://www.elastic.co/products/elasticsearch" \\
  org.opencontainers.image.vendor="Renameme" \\
  org.opencontainers.image.version="${version}"
<% if (docker_base == 'ubi') { %>
LABEL name="Renameme" \\
  maintainer="infra@elastic.co" \\
  vendor="Renameme" \\
  version="${version}" \\
  release="1" \\
  summary="Renameme" \\
  description="You know, for open search."

RUN mkdir /licenses && \\
    cp LICENSE.txt /licenses/LICENSE
<% } %>

ENTRYPOINT ["/tini", "--", "/usr/local/bin/docker-entrypoint.sh"]
# Dummy overridable parameter parsed by entrypoint
CMD ["eswrapper"]

################################################################################
# End of multi-stage Dockerfile
################################################################################
