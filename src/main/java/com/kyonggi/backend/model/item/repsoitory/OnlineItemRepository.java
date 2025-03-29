package com.kyonggi.backend.model.item.repsoitory;

import com.kyonggi.backend.model.item.OnlineItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnlineItemRepository extends JpaRepository<OnlineItem, Integer> {
}
