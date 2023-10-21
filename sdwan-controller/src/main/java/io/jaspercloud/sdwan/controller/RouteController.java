package io.jaspercloud.sdwan.controller;

import io.jaspercloud.sdwan.controller.param.Result;
import io.jaspercloud.sdwan.service.ConfigService;
import io.jaspercloud.sdwan.controller.param.RouteDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Resource
    private ConfigService configService;

    @PostMapping("/save")
    public Result save(@RequestBody RouteDTO request) {
        configService.saveRoute(request);
        return Result.OK;
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        configService.deleteRoute(id);
        return Result.OK;
    }

    @PutMapping
    public Result update(@RequestBody RouteDTO request) {
        configService.updateRoute(request);
        return Result.OK;
    }

    @GetMapping("/list")
    public Result list() {
        List<RouteDTO> routeList = configService.getRouteList();
        return new Result(0, "ok", routeList);
    }
}
