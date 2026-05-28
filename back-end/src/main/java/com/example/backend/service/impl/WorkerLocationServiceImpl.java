package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerLocationUpdateRequest;
import com.example.backend.model.worker.WorkerLocationUpdateResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.service.TechnicianServiceAreasService;
import com.example.backend.service.WorkerLocationService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class WorkerLocationServiceImpl implements WorkerLocationService {

    private static final String DEFAULT_AREA_NAME = "默认服务区域";

    private static final Set<String> MUNICIPALITY_OR_SAR_NAMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "北京市",
            "天津市",
            "上海市",
            "重庆市",
            "香港特别行政区",
            "澳门特别行政区"
        ))
    );

    private static final String[] PROVINCE_SUFFIXES = {"特别行政区", "自治区", "省", "市"};
    private static final String[] CITY_SUFFIXES = {"自治州", "地区", "盟", "市"};
    private static final String[] COUNTY_SUFFIXES = {"自治县", "自治旗", "矿区", "林区", "特区", "县", "区", "旗", "市"};

    @Value("${tencent.map.key:}")
    private String tencentMapKey;

    private final TechnicianServiceAreasService technicianServiceAreasService;
    private final TechnicianProfilesService technicianProfilesService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkerLocationServiceImpl(
        TechnicianServiceAreasService technicianServiceAreasService,
        TechnicianProfilesService technicianProfilesService
    ) {
        this.technicianServiceAreasService = technicianServiceAreasService;
        this.technicianProfilesService = technicianProfilesService;
    }

    @Override
    public WorkerLocationUpdateResponse updateCurrentWorkerLocation(WorkerLocationUpdateRequest request) {
        LoginUserInfo user = requireWorker();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }

        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经纬度不能为空");
        }
        if (!isValidLatLng(latitude, longitude)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的经纬度");
        }

        ReverseGeocodeResult geocode = null;
        String resolvedAddress;
        if (StringUtils.hasText(request.getAddress())) {
            resolvedAddress = request.getAddress().trim();
            geocode = tryReverseGeocode(latitude, longitude, request.getCoordType());
        } else {
            geocode = reverseGeocode(latitude, longitude, request.getCoordType());
            resolvedAddress = geocode.address;
        }

        long now = System.currentTimeMillis();
        String accountId = user.getAccountId();
        TechnicianServiceAreas area = getOrCreateDefaultArea(accountId, now);
        area.setCenterLatitude(latitude);
        area.setCenterLongitude(longitude);
        if (StringUtils.hasText(resolvedAddress)) {
            area.setCenterAddress(resolvedAddress);
        }
        area.setAreaName(resolveAreaName(geocode, resolvedAddress, area.getAreaName()));
        area.setIsActive(1);
        area.setUpdatedTime(now);
        technicianServiceAreasService.updateById(area);

        TechnicianProfiles profile = technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, accountId)
                .eq(TechnicianProfiles::getIsDelete, 0),
            false
        );
        if (profile != null) {
            profile.setLocationUpdateTime(now);
            profile.setUpdatedTime(now);
            technicianProfilesService.updateById(profile);
        }

        WorkerLocationUpdateResponse response = new WorkerLocationUpdateResponse();
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setAddress(resolvedAddress);
        response.setAreaName(area.getAreaName());
        if (geocode != null) {
            response.setProvince(geocode.province);
            response.setCity(geocode.city);
            response.setDistrict(geocode.district);
        }
        response.setLocationUpdateTime(now);
        return response;
    }

    private TechnicianServiceAreas getOrCreateDefaultArea(String accountId, long now) {
        TechnicianServiceAreas area = technicianServiceAreasService.getOne(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .eq(TechnicianServiceAreas::getTechnicianAccountId, accountId)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (area != null) {
            return area;
        }

        TechnicianServiceAreas created = new TechnicianServiceAreas();
        created.setId(SnowflakeIdUtil.nextTechnicianServiceAreaId());
        created.setTechnicianAccountId(accountId);
        created.setCenterLatitude(BigDecimal.ZERO);
        created.setCenterLongitude(BigDecimal.ZERO);
        created.setCenterAddress("");
        created.setAreaName(DEFAULT_AREA_NAME);
        created.setIsDefault(1);
        created.setIsActive(1);
        created.setCreatedTime(now);
        created.setUpdatedTime(now);
        created.setIsDelete(0);
        technicianServiceAreasService.save(created);
        return created;
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        return user;
    }

    private boolean isValidLatLng(BigDecimal lat, BigDecimal lng) {
        return lat.compareTo(BigDecimal.valueOf(-90)) >= 0
            && lat.compareTo(BigDecimal.valueOf(90)) <= 0
            && lng.compareTo(BigDecimal.valueOf(-180)) >= 0
            && lng.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private ReverseGeocodeResult tryReverseGeocode(BigDecimal lat, BigDecimal lng, String coordType) {
        try {
            return reverseGeocode(lat, lng, coordType);
        } catch (BusinessException e) {
            return null;
        }
    }

    private String resolveAreaName(ReverseGeocodeResult geocode, String fullAddress, String oldAreaName) {
        String areaNameFromComponent = buildAreaNameFromComponents(
            geocode == null ? null : geocode.province,
            geocode == null ? null : geocode.city,
            geocode == null ? null : geocode.district
        );
        if (StringUtils.hasText(areaNameFromComponent)) {
            return areaNameFromComponent;
        }

        String areaNameFromAddress = extractAreaNameFromAddress(fullAddress);
        if (StringUtils.hasText(areaNameFromAddress)) {
            return areaNameFromAddress;
        }

        if (StringUtils.hasText(oldAreaName)) {
            return oldAreaName;
        }
        return DEFAULT_AREA_NAME;
    }

    private String buildAreaNameFromComponents(String province, String city, String district) {
        String p = cleanRegionPart(province);
        String c = cleanRegionPart(city);
        String d = cleanRegionPart(district);

        if ("市辖区".equals(d)) {
            d = null;
        }

        if (!StringUtils.hasText(p) && !StringUtils.hasText(c) && !StringUtils.hasText(d)) {
            return null;
        }

        String root = StringUtils.hasText(p) ? p : c;
        if (isMunicipalityOrSar(root)) {
            if (StringUtils.hasText(d) && !d.equals(root)) {
                return root + d;
            }
            return root;
        }

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(p)) {
            sb.append(p);
        }
        if (StringUtils.hasText(c) && !c.equals(p)) {
            sb.append(c);
        }
        if (StringUtils.hasText(d) && !d.equals(c)) {
            sb.append(d);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String extractAreaNameFromAddress(String address) {
        String normalized = normalizeAddress(address);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        String level1 = extractSegment(normalized, PROVINCE_SUFFIXES);
        if (!StringUtils.hasText(level1)) {
            String city = extractSegment(normalized, CITY_SUFFIXES);
            if (!StringUtils.hasText(city)) {
                return null;
            }
            String remain = normalized.substring(city.length());
            String county = extractSegment(remain, COUNTY_SUFFIXES);
            return StringUtils.hasText(county) ? city + county : city;
        }

        String remainAfterLevel1 = normalized.substring(level1.length());
        if (isMunicipalityOrSar(level1)) {
            String county = extractSegment(remainAfterLevel1, COUNTY_SUFFIXES);
            return StringUtils.hasText(county) ? level1 + county : level1;
        }

        String level2 = extractSegment(remainAfterLevel1, CITY_SUFFIXES);
        if (!StringUtils.hasText(level2)) {
            String county = extractSegment(remainAfterLevel1, COUNTY_SUFFIXES);
            return StringUtils.hasText(county) ? level1 + county : level1;
        }

        String remainAfterLevel2 = remainAfterLevel1.substring(level2.length());
        String level3 = extractSegment(remainAfterLevel2, COUNTY_SUFFIXES);
        return StringUtils.hasText(level3) ? level1 + level2 + level3 : level1 + level2;
    }

    private String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String normalized = address.trim()
            .replace(" ", "")
            .replace("\t", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace("，", "")
            .replace(",", "");
        if (normalized.startsWith("中国")) {
            normalized = normalized.substring(2);
        }
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String extractSegment(String source, String[] suffixes) {
        if (!StringUtils.hasText(source) || suffixes == null || suffixes.length == 0) {
            return null;
        }
        int endIndex = Integer.MAX_VALUE;
        for (String suffix : suffixes) {
            if (!StringUtils.hasText(suffix)) {
                continue;
            }
            int idx = source.indexOf(suffix);
            if (idx >= 0) {
                int candidate = idx + suffix.length();
                if (candidate < endIndex) {
                    endIndex = candidate;
                }
            }
        }
        if (endIndex == Integer.MAX_VALUE || endIndex <= 0 || endIndex > source.length()) {
            return null;
        }
        String segment = source.substring(0, endIndex);
        return StringUtils.hasText(segment) ? segment : null;
    }

    private String cleanRegionPart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim().replace(" ", "");
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    private boolean isMunicipalityOrSar(String level1Region) {
        return StringUtils.hasText(level1Region) && MUNICIPALITY_OR_SAR_NAMES.contains(level1Region);
    }

    private ReverseGeocodeResult reverseGeocode(BigDecimal lat, BigDecimal lng, String coordType) {
        if (!StringUtils.hasText(tencentMapKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "腾讯地图Key未配置");
        }

        String url = UriComponentsBuilder
            .fromHttpUrl("https://apis.map.qq.com/ws/geocoder/v1/")
            .queryParam("key", tencentMapKey)
            .queryParam("location", lat + "," + lng)
            .toUriString();

        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }

        try {
            Map<?, ?> root = objectMapper.readValue(body, Map.class);
            Object status = root.get("status");
            if (!(status instanceof Number) || ((Number) status).intValue() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }
            Object resultObj = root.get("result");
            if (!(resultObj instanceof Map)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }

            Map<?, ?> result = (Map<?, ?>) resultObj;
            String address = asText(result.get("address"));

            String province = null;
            String city = null;
            String district = null;
            Object componentObj = result.get("address_component");
            if (componentObj instanceof Map) {
                Map<?, ?> component = (Map<?, ?>) componentObj;
                province = asText(component.get("province"));
                city = asText(component.get("city"));
                district = asText(component.get("district"));
            }

            ReverseGeocodeResult ret = new ReverseGeocodeResult();
            ret.address = address;
            ret.province = province;
            ret.city = city;
            ret.district = district;
            return ret;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }

    private static class ReverseGeocodeResult {
        private String address;
        private String province;
        private String city;
        private String district;
    }
}
