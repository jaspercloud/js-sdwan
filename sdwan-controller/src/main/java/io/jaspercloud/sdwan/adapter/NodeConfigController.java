package io.jaspercloud.sdwan.adapter;

import io.jaspercloud.sdwan.app.ConfigService;
import io.jaspercloud.sdwan.app.NodeDTO;
import io.jaspercloud.sdwan.app.RouteDTO;
import io.jaspercloud.sdwan.domian.Node;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/nodeConfig")
public class NodeConfigController {

    @Resource
    private ConfigService configService;

    @PostMapping("/save")
    public Result save(@RequestBody NodeDTO request) {
        configService.saveNode(request);
        return Result.OK;
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        configService.deleteNode(id);
        return Result.OK;
    }

    @GetMapping("/list")
    public Result list() {
        List<Node> nodeList = configService.getNodeList();
        return new Result(0, "ok", nodeList);
    }
}
