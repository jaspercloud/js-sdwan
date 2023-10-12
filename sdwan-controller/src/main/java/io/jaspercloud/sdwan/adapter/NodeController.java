package io.jaspercloud.sdwan.adapter;

import io.jaspercloud.sdwan.domian.Node;
import io.jaspercloud.sdwan.app.NodeManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Resource
    private NodeManager nodeManager;

    @GetMapping("/list")
    public Result list() {
        List<Node> nodeList = nodeManager.getNodeList();
        return new Result(0, "ok", nodeList);
    }

}
