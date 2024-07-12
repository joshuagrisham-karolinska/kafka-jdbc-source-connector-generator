# Manual OpenShift Deployment

1. Build the image locally while setting the right group+image+tag (update tag version as desired):

```sh
export APP_IMAGE_TAG=0.0.0-alpha.0
mvn package -Dquarkus.container-image.group=artifactory.karolinska.se/kar-vdp-karda-events-images-snapshot -Dquarkus.container-image.name=connector-config-app -Dquarkus.container-image.tag=${APP_IMAGE_TAG}
```

2. Test that it seems to be working:

```sh
docker run --rm -p 8080:8080 artifactory.karolinska.se/kar-vdp-karda-events-images-snapshot/connector-config-app:${APP_IMAGE_TAG}
```

Then open a browser to <http://localhost:8080/querier>.

3. Push the locally-built image to Artifactory:

```sh
docker push artifactory.karolinska.se/kar-vdp-karda-events-images-snapshot/connector-config-app:${APP_IMAGE_TAG}
```

4. Deploy the following resources to OpenShift in the desired namespace:

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: connector-config-app
  name: connector-config-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: connector-config-app
  template:
    metadata:
      labels:
        app: connector-config-app
    spec:
      containers:
        - name: connector-config-app
          image: artifactory.karolinska.se/kar-vdp-karda-events-images-snapshot/connector-config-app:0.0.0-alpha.0
          ports:
            - containerPort: 8080
              name: http
              protocol: TCP
          livenessProbe:
            httpGet:
              path: /querier
              port: 8080
          readinessProbe:
            httpGet:
              path: /querier
              port: 8080
          startupProbe:
            httpGet:
              path: /querier
              port: 8080
          resources:
            limits:
              cpu: 500m
              memory: 1Gi
            requests:
              cpu: 50m
              memory: 256Mi
---
apiVersion: v1
kind: Service
metadata:
  name: connector-config-app
spec:
  selector:
    app: connector-config-app
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 8080
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: connector-config-app-rewrite-root
  annotations:
    haproxy.router.openshift.io/rewrite-target: /querier
spec:
  host: karda-events-connector-config-dev.apps.tamarin.mta.karolinska.se
  port:
    targetPort: http
  tls:
    termination: edge
  to:
    kind: Service
    name: connector-config-app
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: connector-config-app-querier
spec:
  host: karda-events-connector-config-dev.apps.tamarin.mta.karolinska.se
  path: /querier
  port:
    targetPort: http
  tls:
    termination: edge
  to:
    kind: Service
    name: connector-config-app
---
kind: NetworkPolicy
apiVersion: networking.k8s.io/v1
metadata:
  name: temp-allow-incoming-ingress-traffic
spec:
  podSelector: {}
  ingress:
    - from:
      - namespaceSelector:
          matchLabels:
            network.openshift.io/policy-group: ingress
  policyTypes:
    - Ingress
```
