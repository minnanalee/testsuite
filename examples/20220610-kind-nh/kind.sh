kind delete cluster
kind create cluster --config=kind.yaml
kind load docker-image nocalhost-docker.pkg.coding.net/nocalhost/public/nocalhost-sidecar:sshversion
kind load docker-image nocalhost-docker.pkg.coding.net/nocalhost/dev-images/golang:latest
