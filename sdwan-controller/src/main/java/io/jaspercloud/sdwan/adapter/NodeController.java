package io.jaspercloud.sdwan.adapter;

import io.jaspercloud.sdwan.app.ConfigService;
import io.jaspercloud.sdwan.app.NodeDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/node")
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
}
