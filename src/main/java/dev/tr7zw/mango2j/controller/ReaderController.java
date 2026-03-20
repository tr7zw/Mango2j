package dev.tr7zw.mango2j.controller;

import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.service.*;
import dev.tr7zw.mango2j.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.time.*;
import java.util.*;

@Controller
public class ReaderController {

    private final FileService fileService;
    private final TitleRepository titleRepo;
    private final ChapterRepository chapterRepo;
    private final MoveTargetService moveTargetService;
    private StatusUtil statusUtil;

    @Autowired
    public ReaderController(FileService fileService, TitleRepository titleRepo, ChapterRepository chapterRepo,
                            MoveTargetService moveTargetService, StatusUtil statusUtil) {
        this.fileService = fileService;
        this.titleRepo = titleRepo;
        this.chapterRepo = chapterRepo;
        this.moveTargetService = moveTargetService;
        this.statusUtil = statusUtil;
    }

    @GetMapping("/reader/{id}")
    public String home(@PathVariable Integer id, Model model) {
        // Add necessary attributes to the model
        model.addAttribute("mode", "continuous");
        Chapter chapter = chapterRepo.getReferenceById(id);
        chapter.setViews((chapter.getViews() == null ? 0 : chapter.getViews()) + 1);
        chapter.setLastView(Instant.now());
        chapterRepo.save(chapter);
        Title title = titleRepo.findByFullPath(chapter.getPath());
        ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
        model.addAttribute("items", chapterWrapper.getFilesTyped());
        model.addAttribute("titleid", "123");
        model.addAttribute("entryid", id);
        model.addAttribute("page_idx", 0);
        model.addAttribute("margin", 0);
        model.addAttribute("base_url", "http://localhost:8080");
        model.addAttribute("exit_url", "/library/" + title.getId());
        model.addAttribute("delete_url", "/admin/delete/" + chapter.getId());
        model.addAttribute("scanStatus", statusUtil.getScanStatus());

        List<MoveTarget> moveTargets = new ArrayList<>();
        for (int i = 0; i < moveTargetService.getMoveTargets().size(); i++) {
            File target = moveTargetService.getMoveTargets().get(i);
            moveTargets.add(new MoveTarget(target.getName(), "/admin/move/" + id + "/" + i));
        }
        model.addAttribute("moveTargets", moveTargets);

        return "reader";
    }

    private record MoveTarget(String name, String url) {
    }

}
