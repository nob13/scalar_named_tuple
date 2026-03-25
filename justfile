all: build

build:
  ./mill _.compile

presentation_deps:
  cd presentation; pnpm install

presentation_start:
  cd presentation; npm run dev

presentation_build:
  cd presentation; npm run build

export: presentation_pdf repo_zip

presentation_pdf:
  cd presentation; npm run export -- --per-slide
  rm -f scalar_named_tuples.pdf
  mv presentation/slides-export.pdf scalar_named_tuples.pdf

repo_zip:
  rm -f scalar_named_tuples.zip
  git archive --format=zip --output scalar_named_tuples.zip HEAD
  echo "Created scalar_named_tuples.zip"
