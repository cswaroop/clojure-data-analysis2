dat <- data.frame(q1=sample(c("A","B","C"),
			    size=1000,replace=TRUE),
		  sex=sample(c("M","F"),
			     size=1000,replace=TRUE))
dtab <- with(dat,table(q1,sex))

(Xsq <- chisq.test(dtab))
