#! /usr/bin/env python

import sys
import os
import csv
import errno
from optparse import OptionParser
import shutil

def formatListAsRanking(ranks):
	mails = ""
	sc = ""
	firstnames = ""
	lastnames = ""
	
	for user in ranks:
		mails += (len(mails) == 0 and '\"{0}\"'.format(user[0])) or ',\"{0}\"'.format(user[0])
		sc += (len(sc) == 0 and '\"{0}\"'.format(user[1])) or ',\"{0}\"'.format(user[1])
		firstnames += (len(firstnames) == 0 and '\"{0}\"'.format(user[2])) or ',\"{0}\"'.format(user[2])
		lastnames += (len(lastnames) == 0 and '\"{0}\"'.format(user[3])) or ',\"{0}\"'.format(user[3])		
		
	if len(ranks) == 0: # if no rank, JSON object will be 'null'
		mails = "null"
		sc = "null"
		firstnames = "null"
		lastnames = "null"
		
	return mails,sc,firstnames,lastnames	

################### end of formatListAsRanking ##########################################

def getbeforeandafter(mail, scores):
	before = []
	after = []
	score_indx = 0
	must_find = True
	for score in scores:
		user_indx = 0
		for user in score:
			if user[0] == mail:
				must_find = False
			if must_find:
				user_indx += 1
		if must_find:
			score_indx += 1
		else:
			break	
			
	# add before
	u_indx = user_indx
	s_indx = score_indx
	mustadd = True
	while(mustadd):		
		score = scores[s_indx]
		
		u_indx -= 1
		if u_indx < 0:
			s_indx -= 1			
			if s_indx < 0:
				mustadd = False
				break
			u_indx = len(scores[s_indx])-1
		before.insert(0,scores[s_indx][u_indx])
		if len(before) >= 5:
			break

	# add after
	u_indx = user_indx
	s_indx = score_indx
	mustadd = True
	while(mustadd):		
		score = scores[s_indx]
		u_indx += 1
		if u_indx >= len(score):
			s_indx += 1			
			if s_indx >= len(scores):
				mustadd = False
				break
			u_indx = 0
		after.append(scores[s_indx][u_indx])
		if len(after) >= 5:
			break	
	return before,after
	
################### end of getbeforeandafter ##########################################		
	
usage = "usage: prepare_test.py [options] nb_users"
parser = OptionParser(usage=usage)

parser.add_option("-i","--injectors",dest="injectors",type="int",default=1,help="The number of injectors which will be used. (default = 1)")
parser.add_option("-r","--round",dest="round_file",default='round_data.csv',metavar="FILE",help="The name of the file containing round data. (default = 'round_data.csv')")
parser.add_option("-a","--rank",dest="rank_file",default='rank_data.csv',metavar="FILE",help="The name of the file containing rank data. (default = 'rank_data.csv')")
parser.add_option("-t","--top", dest="top_score_file", default='top_scores.csv',metavar="FILE",help="The name of the file containing top scores. (default = 'top_scores.csv')")
(options, args) = parser.parse_args()

if len(args) == 0:
	print usage
	sys.exit()
	
nb_users = int(args[0])

print 'NB users : %d' % nb_users
print 'NB injectors : %d' % options.injectors

nb_users_per_inj = []
for i in range(0,options.injectors):
	if i==0:
		nb_users_per_inj.append(nb_users/options.injectors+nb_users%options.injectors)
	else:
		nb_users_per_inj.append(nb_users/options.injectors)
	inj_dir = 'injector_'+str(i+1) 
	os.listdir('.').count(inj_dir) == 0 and os.mkdir(inj_dir)
	os.listdir(inj_dir).count('data') == 0 and os.mkdir(inj_dir+'/data')
			
print 'NB users per injector : %s' % nb_users_per_inj

#sort rank
print 'Ordering data of players ...'
f_rank = file(options.rank_file,'r')
ranks=csv.reader(f_rank,dialect='excel')

score_list = {}

cnt = 0
for rank in ranks:
	if cnt > 0:
		current_score = int(rank[1])
		score_set = score_list.get(current_score)
		if not score_set:
			score_set = []
			score_list[current_score] = score_set
		score_set.append(rank)
		print 'process user %s' % cnt 
	cnt += 1
	if cnt > nb_users:
		break

sorted_scores = score_list.keys()
sorted_scores.sort()
sorted_scores.reverse()
	
cnt = 0
scores = []
for score in sorted_scores:
	print 'process score %s on %s' % (cnt,len(sorted_scores)) 
	score_set = score_list.get(score)
	score_set.sort(cmp=lambda x,y: cmp(x[3], y[3])!=0 and cmp(x[3], y[3]) or cmp(x[2], y[2])!=0 and cmp(x[2], y[2]) or cmp(x[0], y[0]))	
	scores.append(score_set)
	cnt += 1
	
f_rank.close()
print 'Order done.'

print 'Generate top user file...'
top_ranks = []
cnt = 0
for score in scores:
	for user in score:
		top_ranks.append(user)
		cnt += 1
		if cnt > 100:			
			break
	if cnt > 100:
			break
			
f_top = file('injector_1/data/'+options.top_score_file,'w')
mails,sc,firstnames,lastnames = formatListAsRanking(top_ranks)
f_top.write(".*mail.*{0}.*scores.*{1}.*firstname.*{2}.*lastname.*{3}.*".format(mails, sc, firstnames,lastnames))	
f_top.close()
for i in range(1,options.injectors):
	inj_dir = 'injector_'+str(i+1)
	shutil.copy(options.top_score_file, inj_dir+'/data/'+options.top_score_file)
print 'Done.'

#split round file according to nb injecteurs
print 'Split round file ...'
f_users = file(options.round_file,'r')

inj_indx = 0
user_indx = 0
f_current = None
nb_user_in_inj = 1
user = f_users.readline()
while( len(user) != 0):	
	if user_indx != 0:
		if nb_user_in_inj > nb_users_per_inj[inj_indx]:
			f_current.close()
			f_current = None
			inj_indx += 1
			if inj_indx >= options.injectors:
				break

		if f_current == None:
			nb_user_in_inj = 1
			f_current = file('injector_'+str(inj_indx+1)+'/data/round_data.csv','w')

		if user_indx > nb_users:
			break
			
		data = user.split(',')
		user = user.replace(',','%%')
		user = user.replace('\n','')
		before, after = getbeforeandafter(data[0], scores)
		before_mails,before_sc,before_firstnames,before_lastnames = formatListAsRanking(before)
		after_mails,after_sc,after_firstnames,after_lastnames = formatListAsRanking(after)
		f_current.write(user)
		f_current.write('%%{0}%%{1}%%{2}%%{3}%%{4}%%{5}%%{6}%%{7}\n'.format(before_mails,before_sc,before_firstnames,before_lastnames,after_mails,after_sc,after_firstnames,after_lastnames))
		nb_user_in_inj += 1
	user_indx += 1
	print 'Rank file for user %s' % (user_indx)
	user = f_users.readline()

f_users.close()
print 'Done.'