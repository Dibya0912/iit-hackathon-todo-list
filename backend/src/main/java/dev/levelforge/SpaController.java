package dev.levelforge;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/quests","/leaderboard","/shop","/inventory","/profile"})
    String app(){return "forward:/index.html";}
}
