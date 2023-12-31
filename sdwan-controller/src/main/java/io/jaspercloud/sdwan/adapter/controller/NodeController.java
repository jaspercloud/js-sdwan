package io.jaspercloud.sdwan.adapter.controller;

import io.jaspercloud.sdwan.adapter.controller.param.NodeDTO;
import io.jaspercloud.sdwan.adapter.controller.param.Result;
import io.jaspercloud.sdwan.domain.control.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/node")
public class NodeController {

    @Resource
    private ConfigService configService;

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        configService.deleteNode(id);
        return Result.OK;
    }

    @PutMapping
    public Result update(@RequestBody NodeDTO request) {
        configService.updateNode(request);
        return Result.OK;
    }

    @GetMapping("/list")
    public Result list() {
        List<NodeDTO> nodeList = configService.getNodeList();
        return new Result(0, "ok", nodeList);
    }

    @GetMapping("/meshList")
    public Result meshList() {
        List<NodeDTO> nodeList = configService.getMeshNodeList();
        return new Result(0, "ok", nodeList);
    }

    @PostMapping("/{id}/disconnect")
    public Result disconnect(@PathVariable("id") Long id) {
        configService.disconnectNode(id);
        return Result.OK;
    }
}
