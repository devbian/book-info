package com.pera.controller;

import com.pera.entity.Book;
import com.pera.service.BookService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import java.io.IOException;
import java.util.List;

/**
 * 使用京东的图书搜索信息
 * 首先查找由京东发货的图书，这种情况下，图书信息更加的完整，全面
 * Created by phnix on 11/17/2014.
 */
@Controller
@RequestMapping("/")
public class ISBNSearchController {
    @Autowired
    BookService bookService;

    @RequestMapping(value = "search/{isbn}")
    @ResponseBody
    public ResponseEntity<Book> search(@PathVariable("isbn") String isbnString) throws IOException {
        String title;
        Book bookObject = bookService.findByIsbn(isbnString);
        if (bookObject != null){
            return new ResponseEntity<Book>(bookObject, HttpStatus.OK);
        }
        String searchUrl = "http://search.jd.com/Search?keyword=" + isbnString;
        System.out.println(searchUrl);
        Document document = Jsoup.connect(searchUrl).timeout(10000).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.120 Safari/535.2").get();

        Elements books = document.select("#plist ul li.item-book");
        Element marchBook = null;
        for (Element book : books) {
            if (book.select(".service").first().text().equals("由 京东 发货")
                    && book.select(".p-img .pi-ebook").size()==0){
                marchBook = book;
                break;
            }
        }
        if (marchBook == null) {
            marchBook = books.first();
        }
        String bookUrl = marchBook.select("div a").attr("href");
        Document bookDoc = Jsoup.connect(bookUrl).timeout(10000).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.120 Safari/535.2").get();
        Elements elements = bookDoc.select("#product-detail-1 ul li");
        Book book = new Book();
        title = bookDoc.select("#name h1").first().ownText();
        book.setTitle(title);
        String author = bookDoc.select("#product-authorinfo").text();
        book.setImage(bookDoc.select("#spec-n1 img").attr("src"));
        book.setAuthor(author);
        for (Element element : elements) {
            String liStr = element.text();
            if (liStr.indexOf("ISBN") != -1) {
                book.setIsbn(liStr.substring(5));
                System.out.println(book.getIsbn());
            } else if (liStr.indexOf("出版社") != -1) {
                book.setPress(liStr.substring(4));
            }else if(liStr.indexOf("页数") != -1) {
                book.setPages(liStr.substring(3));
            }
        }
        bookService.save(book);
        return new ResponseEntity<Book>(book, HttpStatus.OK);
    }

    @RequestMapping(value = "mark/{isbn}")
    @ResponseBody
    public boolean recordBook(@PathVariable(value = "isbn") String isbn){
        Book book = bookService.findByIsbn(isbn);
        book.setRecord(true);
        bookService.save(book);
        return true;
    }

    @RequestMapping(value = "all")
    public ResponseEntity<List<Book>> findAll(){
        List<Book> books = bookService.findAll();
        return new ResponseEntity<List<Book>>(books,HttpStatus.OK);
    }
}
