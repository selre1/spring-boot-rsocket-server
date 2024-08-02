package com.bhb.server;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Controller
public class RSocketService {
    private final ItemRepository itemRepository;

    private final Sinks.Many<Item> itemMany;

    public RSocketService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        this.itemMany = Sinks.many().multicast().onBackpressureBuffer();
    }

    /*
    * tryEmitNext sink에서 새로 저장된 아이템을 추가
    *  monitorNewItem 메서드에서 구독중인 클라이언트들이 새로추가된 아이템을 실시간으로 받게 된다.
    * */

    @MessageMapping("newItems.req-res")
    public Mono<Item> processNewItems(Item item){
        return itemRepository.save(item)
                .doOnNext(itemMany::tryEmitNext);
    }

    @MessageMapping("newItems.req-stream")
    public Flux<Item> processStream(){
        return itemRepository.findAll()
                .doOnNext(itemMany::tryEmitNext);
    }

    @MessageMapping("newItems.fire-and-forget") //실행 후 망각
    public Mono<Void> processFireAndForget(Item item){
        return itemRepository.save(item)
                .doOnNext(itemMany::tryEmitNext).then();
    }

    @MessageMapping("newItems.monitor")
    public Flux<Item> monitorNewItem(){
        return itemMany.asFlux();
    }
}
