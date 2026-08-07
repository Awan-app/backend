package com.ezdo.service;

import com.ezdo.entity.Item;
import com.ezdo.entity.ItemType;
import com.ezdo.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSeedService implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(String... args) throws Exception {
        if (itemRepository.count() == 0) {
            log.info("Store is empty. Seeding default gamification items...");

            List<Item> defaultItems = List.of(
                // Frames
                Item.builder()
                        .name("Gold Frame")
                        .description("A shiny gold frame for your profile.")
                        .price(100)
                        .type(ItemType.FRAME)
                        .version("1.0")
                        .image("https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png")
                        .build(),
                Item.builder()
                        .name("Diamond Frame")
                        .description("An exclusive diamond frame.")
                        .price(500)
                        .type(ItemType.FRAME)
                        .version("1.0")
                        .image("https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/diamond_frame.png")
                        .build(),
                
                // Themes
                Item.builder()
                        .name("Dark Mode Theme")
                        .description("A sleek dark theme for the app.")
                        .price(200)
                        .type(ItemType.THEME)
                        .version("1.0")
                        .image("https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/dark_theme.png")
                        .build(),
                Item.builder()
                        .name("Cyberpunk Theme")
                        .description("A futuristic cyberpunk aesthetic.")
                        .price(1000)
                        .type(ItemType.THEME)
                        .version("1.0")
                        .image("https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/cyberpunk_theme.png")
                        .build(),

                // Skins
                Item.builder()
                        .name("Robot Avatar")
                        .description("Change your avatar to a robot.")
                        .price(300)
                        .type(ItemType.SKIN)
                        .version("1.0")
                        .image("https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/robot_avatar.png")
                        .build()
            );

            itemRepository.saveAll(defaultItems);
            log.info("Successfully seeded {} items.", defaultItems.size());
        } else {
            log.info("Store already contains items. Skipping seeding.");
        }
    }
}
