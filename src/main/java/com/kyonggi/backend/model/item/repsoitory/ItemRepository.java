package com.kyonggi.backend.model.item.repsoitory;

import com.kyonggi.backend.model.item.Item;
import org.springframework.data.repository.CrudRepository;

public interface ItemRepository extends CrudRepository<Item, Long> {
}
