package com.graalvmonlambda.infra;

import java.util.Arrays;
import java.util.Map;
import java.util.List;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegrationProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

public class InfrastructureStack extends Stack {

    public InfrastructureStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public InfrastructureStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        List<String> functionOnePackagingInstructions = Arrays.asList(
                "-c",
                "cd products " +
                        "&& mvn clean install -P  native-image"
                       + "&& cp /asset-input/products/target/function.zip /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(functionOnePackagingInstructions)
                .image(DockerImage.fromRegistry("marksailes/al2-graalvm"))
                .volumes(singletonList(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

      /*  Function productFunction = new Function(this, "ProductFunction", FunctionProps.builder()
                .runtime(Runtime.PROVIDED_AL2)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions.build())
                        .build()))
                .handler("com.graalvmonlambda.product.ProductRequestHandler")
                .memorySize(256)
                .logRetention(RetentionDays.ONE_WEEK)
                .build());*/

        HttpApi httpApi = new HttpApi(this, "GraalVMOnLambdaAPI", HttpApiProps.builder()
                .apiName("GraalVMonLambdaAPI")
                .build());

      /*  httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/product")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(productFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());*/

        
        Function productFunctionJvm = new Function(this, "ProductFunctionJVM", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/products/target/product.jar"))
                .handler("com.graalvmonlambda.product.ProductRequestHandler")
                .memorySize(2048)
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/product-jvm")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(productFunctionJvm)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());
        
        VpcLookupOptions vpcLookupOption = new VpcLookupOptions.Builder().vpcId("vpc-0df0555da605dbb94").build();
        IVpc rdsVpc = Vpc.fromLookup(this, "my-VPC", vpcLookupOption);
        
        // Create Slots JVM
        Function createSlotsJvm = new Function(this, "CreateSlotsJVM", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/products/target/product.jar"))
                .handler("com.graalvmonlambda.product.CreateSlots")
                .memorySize(2048)
                .environment(
                        Map.of(
                            "DB_ENDPOINT","farmerdbproxy.proxy-crgtil7wvl4r.eu-west-2.rds.amazonaws.com",
                            "DB_REGION", "eu-west-2",
                            "DB_USER", "lambda_iam",
                            "DB_PORT","3306",
                            "DB_ADMIN_SECRET", "DeliveryProjectDbFarmerDBSe-O67BetzHLFhn",
                            "DB_USER_SECRET", "DbUserSecret7350D430-EyPw0HbmDPSY",
                            "CORS_ALLOW_ORIGIN_HEADER", "*")) // TODO: avoid '*'
                .vpc(rdsVpc)
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/createslots-jvm")
                .methods(singletonList(HttpMethod.POST))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(createSlotsJvm)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)	// was 2
                        .build()))
                .build());
        
        
        Function createSlotsGraalVmFunction = new Function(this, "createSlotsGraalVmFunction", FunctionProps.builder()
                .runtime(Runtime.PROVIDED_AL2)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions.build())
                        .build()))
                .handler("com.graalvmonlambda.product.CreateSlots")
                .memorySize(256)
                .environment(
                        Map.of(
                            "DB_ENDPOINT","farmerdbproxy.proxy-crgtil7wvl4r.eu-west-2.rds.amazonaws.com",
                            "DB_REGION", "eu-west-2",
                            "DB_USER", "lambda_iam",
                            "DB_PORT","3306",
                            "DB_ADMIN_SECRET", "DeliveryProjectDbFarmerDBSe-O67BetzHLFhn",
                            "DB_USER_SECRET", "DbUserSecret7350D430-EyPw0HbmDPSY",
                            "CORS_ALLOW_ORIGIN_HEADER", "*")) // TODO: avoid '*'
                .vpc(rdsVpc)
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/createslots-graalvm")
                .methods(singletonList(HttpMethod.POST))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(createSlotsGraalVmFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                        .build()))
                .build());


        CfnOutput apiUrl = new CfnOutput(this, "ProductApiUrl", CfnOutputProps.builder()
                .exportName("ProductApiUrl")
                .value(httpApi.getApiEndpoint())
                .build());
    }
}
