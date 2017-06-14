FROM docker-registry.tools.expedia.com/stratus/primer-base-springboot:8-2

USER root

ENV MAGICK_URL "http://imagemagick.org/download/releases"
ENV MAGICK_VERSION 6.9.1-10

ENV EXIFTOOL_VERSION=10.20

RUN cd /tmp \
  && apt-get update -y \
  && apt-get install -y build-essential perl perl-modules \
  && wget http://www.sno.phy.queensu.ca/~phil/exiftool/Image-ExifTool-${EXIFTOOL_VERSION}.tar.gz \
  && tar -zxvf Image-ExifTool-${EXIFTOOL_VERSION}.tar.gz \
  && cd Image-ExifTool-${EXIFTOOL_VERSION} \
  && perl Makefile.PL \
  && make test \
  && make install \
  && cd .. \
  && rm -rf Image-ExifTool-${EXIFTOOL_VERSION}

RUN gpg --keyserver pool.sks-keyservers.net --recv-keys 8277377A \
  && apt-get update -y \
  && apt-get install -y --no-install-recommends \
    libpng-dev libjpeg-dev libtiff-dev libopenjpeg-dev \
  && apt-get remove -y imagemagick \
  && cd /tmp \
  && curl -SLO "${MAGICK_URL}/ImageMagick-${MAGICK_VERSION}.tar.xz" \
  && curl -SLO "${MAGICK_URL}/ImageMagick-${MAGICK_VERSION}.tar.xz.asc" \
  && gpg --verify "ImageMagick-${MAGICK_VERSION}.tar.xz.asc" "ImageMagick-${MAGICK_VERSION}.tar.xz" \
  && tar xf "ImageMagick-${MAGICK_VERSION}.tar.xz" \

# http://www.imagemagick.org/script/advanced-unix-installation.php#configure
  && cd "ImageMagick-${MAGICK_VERSION}" \
  && ./configure \
    --disable-static \
    --enable-shared \

    --with-jpeg \
    --with-jp2 \
    --with-openjp2 \
    --with-png \
    --with-tiff \
    --with-quantum-depth=8 \

    --without-magick-plus-plus \
    # disable BZLIB support
    --without-bzlib \
    # disable ZLIB support
    --without-zlib \
    # disable Display Postscript support
    --without-dps \
    # disable FFTW support
    --without-fftw \
    # disable FlashPIX support
    --without-fpx \
    # disable DjVu support
    --without-djvu \
    # disable fontconfig support
    --without-fontconfig \
    # disable Freetype support
    --without-freetype \
    # disable JBIG support
    --without-jbig \
    # disable lcms (v1.1X) support
    --without-lcms \
    # disable lcms (v2.X) support
    --without-lcms2 \
    # disable Liquid Rescale support
    --without-lqr \
     # disable LZMA support
    --without-lzma \
    # disable OpenEXR support
    --without-openexr \
    # disable PANGO support
    --without-pango \
    # disable TIFF support
    --without-webp \
    # don't use the X Window System
    --without-x \
    # disable XML support
    --without-xml \

  && make \
  && make install \
  && ldconfig /usr/local/lib \

  && apt-get -y autoclean \
  && apt-get -y autoremove \
  && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ENV APP_NAME=cs-media-service-ng

# Add custom entrypoint script
COPY docker-entrypoint.sh /

# Install application
COPY target/cs-media-service-ng.jar /app/bin/

USER webmaster
