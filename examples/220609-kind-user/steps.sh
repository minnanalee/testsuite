docker cp kind-control-plane:/etc/kubernetes/pki/ca.crt ca.crt
docker cp kind-control-plane:/etc/kubernetes/pki/ca.key ca.key
openssl genrsa -out bob.key 2048
openssl req -new -key bob.key -out bob.csr -subj "/CN=Bob/O=DevTest"
openssl x509 -req -in bob.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out bob.crt -days 999
kubectl config set-cluster dev-cluster --server=https://192.168.1.35:6443 \
--certificate-authority=ca.crt \
--embed-certs=true
kubectl config set-credentials bob --client-certificate=bob.crt --client-key=bob.key --embed-certs=true
kubectl config set-context dev --cluster=dev-cluster --namespace=devops --user=bob
kubectl -n devops apply -f devops-role.yaml
kubectl -n devops apply -f devops-rolebinding.yaml
kubectl config use-context dev

