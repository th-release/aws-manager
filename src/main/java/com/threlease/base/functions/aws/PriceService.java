package com.threlease.base.functions.aws;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClient;
import com.amazonaws.services.pricing.model.Filter;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.threlease.base.utils.EnumStringComparison;
import com.threlease.base.utils.Failable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.ArrayList;
import java.util.List;

@Service
public class PriceService {
    public Failable<Double, String> getTypePricePerHour(
            AWSPricing pricingClient,
            String instanceType
    ) {
        if (EnumStringComparison.compareEnumString(instanceType, InstanceType.class) ||
                (!instanceType.equals("t3a.nano")
                        && !instanceType.equals("t3a.small")
                        && !instanceType.equals("t3a.micro")
                        && !instanceType.equals("t2.nano"))) {
            return Failable.error("Invalid Instance Type");
        }

        List<Filter> filters = new ArrayList<Filter>();

        Filter instanceTypeFilter = new Filter();
        instanceTypeFilter.setType("TERM_MATCH");
        instanceTypeFilter.setField("instanceType");
        instanceTypeFilter.setValue(instanceType);

        filters.add(instanceTypeFilter);

        Filter operatingSystemFilter = new Filter();
        operatingSystemFilter.setType("TERM_MATCH");
        operatingSystemFilter.setField("operatingSystem");
        operatingSystemFilter.setValue("Linux");

        filters.add(operatingSystemFilter);

        Filter tenancyFilter = new Filter();
        tenancyFilter.setType("TERM_MATCH");
        tenancyFilter.setField("tenancy");
        tenancyFilter.setValue("Shared");

        filters.add(tenancyFilter);

        Filter preInstalledSwFilter = new Filter();
        preInstalledSwFilter.setType("TERM_MATCH");
        preInstalledSwFilter.setField("preInstalledSw");
        preInstalledSwFilter.setValue("NA");

        filters.add(preInstalledSwFilter);

        Filter regionCodeFilter = new Filter();
        regionCodeFilter.setType("TERM_MATCH");
        regionCodeFilter.setField("regionCode");
        regionCodeFilter.setValue("ap-northeast-2");

        filters.add(regionCodeFilter);

        Filter capacitystatusFilter = new Filter();
        capacitystatusFilter.setType("TERM_MATCH");
        capacitystatusFilter.setField("capacitystatus");
        capacitystatusFilter.setValue("Used");

        filters.add(capacitystatusFilter);

        GetProductsRequest request = new GetProductsRequest();
        request.setServiceCode("AmazonEC2");
        request.setFilters(filters);

        try {
            GetProductsResult response = pricingClient.getProducts(request);

            if (response.getPriceList().isEmpty())
                return Failable.success(0.0);
            else {
                final double[] price = {0.0F};

                response.getPriceList().forEach((value) -> {
                    price[0] = price[0] + Double.parseDouble(value);
                });

                return Failable.success(price[0]);
            }
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }
}
