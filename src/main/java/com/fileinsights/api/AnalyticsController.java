package com.fileinsights.api;

import com.fileinsights.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Retrieves file data grouped by age.
     *
     * @return A map of age categories to file counts.
     */
    @GetMapping("/by-age")
    public Map<String, Long> getFileDataByAge() {
        return analyticsService.getFileDataByAge();
    }

    /**
     * Retrieves file data grouped by type.
     *
     * @return A map of file types to file counts.
     */
    @GetMapping("/by-type")
    public Map<String, Long> getFileDataByType() {
        return analyticsService.getFileDataByType();
    }
}
