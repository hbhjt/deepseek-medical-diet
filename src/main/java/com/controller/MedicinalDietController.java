package com.controller;

import com.pojo.HealthProfile;
import com.pojo.MedicinalDiet;

import com.service.MedicinalDietService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medicinal-diet")
public class MedicinalDietController {

    @Autowired
    private MedicinalDietService dietService;

    /**
     * 接收用户健康画像，返回推荐的药膳
     */
    @PostMapping("/recommend")
    public ResponseEntity<MedicinalDiet> recommend(@RequestBody HealthProfile profile) {
        try {
            MedicinalDiet recommended = dietService.recommendAndSave(profile);
            return ResponseEntity.ok(recommended);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

}