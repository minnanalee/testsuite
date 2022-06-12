scp root@192.168.1.35:/etc/kubernetes/admin.conf "C:\Users\zhang\.kube\config"
export KUBECONFIG=~/.kube/admin.conf
kubectl config set-context kind-kind --cluster=kind-kind --namespace=devops --user=kind-kind
kubectl port-forward deploy/authors 9019 -n devops

#NO PLUGINS
nhctl dev start bookinfo --deployment authors --local-sync C:\\Users\\zhang\\Downloads\\bookinfo-authors-main\\bookinfo-authors-main --container authors --controller-type Deployment --without-terminal --kubeconfig $KUBECONFIG --namespace devops

nhctl.exe dev terminal bookinfo --deployment authors --kubeconfig $KUBECONFIG --namespace devops --controller-type Deployment --container nocalhost-dev

sh debug.sh

kubectl port-forward deploy/authors 9019 -n devops

nhctl.exe dev end bookinfo --deployment authors --controller-type Deployment --kubeconfig $KUBECONFIG --namespace devops

改代码后kill进程

